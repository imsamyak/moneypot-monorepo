// Runs a whole pot through the real services: identity 9086, wallet 9081, pot 9082.
// Start the containers and the three services first (see the README), then: node bootstrap/local/e2e.mjs
//
// The story: Asha creates a pot that starts in a few seconds. Bala and Chitra join with just the invite code and pay.
// Dev joins, then leaves and gets a full refund. Bidding starts by itself at the start time, nobody can join or leave
// any more, and three rounds run to the end.
import assert from 'node:assert/strict';

const IDENTITY = 'http://localhost:9086/api';
const WALLET = 'http://localhost:9081/api';
const POT = 'http://localhost:9082/api';

async function call(method, url, body, token) {
  const response = await fetch(url, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await response.text();
  return [response.status, text ? JSON.parse(text) : {}];
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function register(name) {
  const email = `${name}${Date.now()}@example.com`;
  const [status, body] = await call('POST', `${IDENTITY}/auth/register`, { email, password: 'correct horse battery' });
  assert.equal(status, 201, JSON.stringify(body));
  const [, me] = await call('GET', `${IDENTITY}/auth/me`, undefined, body.accessToken);
  return [body.accessToken, me.id];
}

async function waitFor(description, check, seconds = 60) {
  const deadline = Date.now() + seconds * 1000;
  while (Date.now() < deadline) {
    const value = await check();
    if (value) return value;
    await sleep(500);
  }
  throw new Error(`timed out waiting for ${description}`);
}

const balance = async (token) => (await call('GET', `${WALLET}/wallet`, undefined, token))[1].balance;
const statusOf = async (...args) => (await call(...args))[0];

const [aToken, aId] = await register('asha');
const [bToken, bId] = await register('bala');
const [cToken, cId] = await register('chitra');
const [dToken] = await register('dev');
const names = { [aId]: 'A', [bId]: 'B', [cId]: 'C' };
const all = [aToken, bToken, cToken, dToken];

for (const token of all) {
  assert.equal(await statusOf('POST', `${WALLET}/wallet`, undefined, token), 200);
  const [status, body] = await call('POST', `${WALLET}/wallet/deposit`, { amount: 500000 }, token);
  assert.ok(status === 200 && body.balance === 500000, JSON.stringify(body));
}

// a pot that starts in 25 seconds; Asha pays her own contribution when she creates it
const startsAt = new Date(Date.now() + 25_000).toISOString();
const [created, pot] = await call('POST', `${POT}/pots`, { name: 'Friends', contribution: 100000, minBidPercent: 60, bidWindowSeconds: 600, startsAt }, aToken);
assert.equal(created, 201, JSON.stringify(pot));
const potId = pot.id;
const code = pot.inviteCode;
console.log('pot', potId, 'invite code', code, 'starts at', startsAt);
assert.equal((await call('GET', `${POT}/pots/${potId}`, undefined, bToken))[1].inviteCode, null); // only the owner sees it

const getPot = async (token = aToken) => (await call('GET', `${POT}/pots/${potId}`, undefined, token))[1];
const pots = (path, body, token) => call('POST', `${POT}/pots/${path}`, body, token);

// the code alone opens the pot (so a person sees what they would pay) and joins it, taking the contribution
assert.equal((await pots('join', { inviteCode: '0000000000000000' }, bToken))[0], 404);
console.log('a wrong code finds nothing (404)');
const [previewStatus, preview] = await call('GET', `${POT}/pots/invite/${code}`, undefined, bToken);
assert.ok(previewStatus === 200 && preview.name === 'Friends' && preview.inviteCode === null, JSON.stringify(preview));
console.log('the code shows the pot:', preview.name, preview.contribution, 'paise each');
assert.equal((await pots(`${potId}/join`, { inviteCode: 'wrong' }, bToken))[0], 403);
for (const token of [bToken, cToken, dToken]) {
  const [status, body] = await pots('join', { inviteCode: code }, token);
  assert.ok(status === 200 && body.id === potId, JSON.stringify(body));
}
await waitFor('everyone paid', async () => (await getPot()).members.filter((m) => m.status === 'ACTIVE').length === 4);
const afterJoining = await Promise.all(all.map(balance));
console.log('balances after joining (A, B, C, D):', afterJoining);
assert.deepEqual(afterJoining, [400000, 400000, 400000, 400000]);

// Dev changes their mind before bidding starts: a full refund
assert.equal((await pots(`${potId}/leave`, undefined, aToken))[0], 422); // the owner cannot leave
assert.equal((await pots(`${potId}/leave`, undefined, dToken))[0], 200);
await waitFor('the refund', async () => (await balance(dToken)) === 500000);
console.log('Dev left and was refunded: balance', await balance(dToken));
assert.equal((await getPot()).memberCount, 3);

// the start time comes: joining closes, bidding opens by itself
await waitFor('bidding to start', async () => {
  const current = await getPot();
  return current.status === 'ACTIVE' && current.round.status === 'BIDDING';
}, 90);
const started = await getPot();
console.log('bidding started: members', started.memberCount, 'pot', started.potAmount, 'lowest bid allowed', started.minBid);
assert.ok(started.memberCount === 3 && started.potAmount === 300000);
assert.equal((await pots('join', { inviteCode: code }, dToken))[0], 422); // joining is closed
assert.equal((await pots(`${potId}/leave`, undefined, bToken))[0], 422); // and nobody can leave

// round 1
assert.equal((await pots(`${potId}/bids`, { amount: 250000 }, aToken))[0], 200);
assert.equal((await pots(`${potId}/bids`, { amount: 240000 }, bToken))[0], 200);
assert.equal((await pots(`${potId}/bids`, { amount: 270000 }, cToken))[0], 200);
const [belowFloor, belowFloorBody] = await pots(`${potId}/bids`, { amount: 100 }, cToken);
assert.equal(belowFloor, 422, JSON.stringify(belowFloorBody));
console.log('a bid below the floor is refused:', belowFloorBody.detail);
assert.equal((await pots(`${potId}/settle`, undefined, aToken))[0], 200);

await waitFor('round 2 open', async () => {
  const current = await getPot();
  return current.currentRound === 2 && ['COLLECTING', 'BIDDING'].includes(current.round.status);
});
const history = (await getPot()).history;
console.log('round 1 history', history.map((h) => [names[h.winnerId], h.payout]));
assert.ok(history[0].winnerId === bId && history[0].payout === 240000);

// round 2: B has already collected and cannot bid again
await waitFor('round 2 bidding', async () => (await getPot()).round.status === 'BIDDING');
const [repeat, repeatBody] = await pots(`${potId}/bids`, { amount: 200000 }, bToken);
assert.equal(repeat, 422, JSON.stringify(repeatBody));
console.log('the collector cannot bid again:', repeatBody.detail);
assert.equal((await pots(`${potId}/bids`, { amount: 280000 }, aToken))[0], 200);
assert.equal((await pots(`${potId}/bids`, { amount: 190000 }, cToken))[0], 200);
assert.equal((await pots(`${potId}/settle`, undefined, aToken))[0], 200);

// round 3: A is the last one left and is paid the whole pot with no bidding
await waitFor('pot completed', async () => (await getPot()).status === 'COMPLETED');
const final = await getPot();
console.log('history', final.history.map((h) => [names[h.winnerId], h.payout]));
const balances = await Promise.all([aToken, bToken, cToken].map(balance));
const sum = balances.reduce((total, value) => total + value, 0);
console.log('final balances A, B, C =', balances, 'sum =', sum);
assert.deepEqual(balances, [585000, 495000, 420000]);
assert.equal(sum, 1500000);
assert.ok(final.members.every((m) => m.won));
assert.equal(final.history.length, 3);

const [, ledger] = await call('GET', `${WALLET}/wallet/ledger?page=0&size=20`, undefined, dToken);
console.log('Dev ledger (newest first):', ledger.items.map((e) => [e.kind, e.type, e.amount]));
console.log('END TO END OK');
