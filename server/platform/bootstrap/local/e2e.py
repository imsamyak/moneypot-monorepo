# Runs a whole pot through the real services: identity 9086, wallet 9081, pot 9082.
# Start the containers and the three services first (see the README), then: python e2e.py
#
# The story: Asha creates a pot that starts in a few seconds. Bala and Chitra join with the invite link and pay.
# Dev joins, then leaves and gets a full refund. Bidding starts by itself at the start time, nobody can join or leave
# any more, and three rounds run to the end.
import json
import time
import urllib.error
import urllib.request
from datetime import datetime, timedelta, timezone

IDENTITY = 'http://localhost:9086/api'
WALLET = 'http://localhost:9081/api'
POT = 'http://localhost:9082/api'


def call(method, url, body=None, token=None):
    data = json.dumps(body).encode() if body is not None else None
    request = urllib.request.Request(url, data=data, method=method)
    request.add_header('Content-Type', 'application/json')
    if token:
        request.add_header('Authorization', 'Bearer ' + token)
    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            text = response.read().decode()
            return response.status, (json.loads(text) if text else {})
    except urllib.error.HTTPError as error:
        text = error.read().decode()
        return error.code, (json.loads(text) if text else {})


def register(name):
    email = '%s%d@example.com' % (name, int(time.time() * 1000))
    status, body = call('POST', IDENTITY + '/auth/register', {'email': email, 'password': 'correct horse battery'})
    assert status == 201, (status, body)
    token = body['accessToken']
    status, me = call('GET', IDENTITY + '/auth/me', token=token)
    return token, me['id']


def wait_for(description, check, seconds=60):
    deadline = time.time() + seconds
    while time.time() < deadline:
        value = check()
        if value:
            return value
        time.sleep(0.5)
    raise AssertionError('timed out waiting for ' + description)


def balance(token):
    return call('GET', WALLET + '/wallet', token=token)[1]['balance']


a_token, a_id = register('asha')
b_token, b_id = register('bala')
c_token, c_id = register('chitra')
d_token, d_id = register('dev')
names = {a_id: 'A', b_id: 'B', c_id: 'C'}

for token in (a_token, b_token, c_token, d_token):
    assert call('POST', WALLET + '/wallet', token=token)[0] == 200
    status, body = call('POST', WALLET + '/wallet/deposit', {'amount': 500000}, token)
    assert status == 200 and body['balance'] == 500000, (status, body)

# a pot that starts in 25 seconds; Asha pays her own contribution when she creates it
starts_at = (datetime.now(timezone.utc) + timedelta(seconds=25)).strftime('%Y-%m-%dT%H:%M:%S.000Z')
status, pot = call('POST', POT + '/pots', {'name': 'Friends', 'contribution': 100000, 'minBidPercent': 60,
                                             'bidWindowSeconds': 600, 'startsAt': starts_at}, a_token)
assert status == 201, (status, pot)
pot_id, code = pot['id'], pot['inviteCode']
print('pot', pot_id, 'invite code', code, 'starts at', starts_at)
assert call('GET', POT + '/pots/%s' % pot_id, token=b_token)[1]['inviteCode'] is None  # only the owner sees it


def get_pot(token=a_token):
    return call('GET', POT + '/pots/%s' % pot_id, token=token)[1]


# joining needs the invitation, and takes the contribution
status, body = call('POST', POT + '/pots/%s/join' % pot_id, {'inviteCode': 'wrong'}, b_token)
assert status == 403, (status, body)
print('without the right code: refused (403)')
for token in (b_token, c_token, d_token):
    status, body = call('POST', POT + '/pots/%s/join' % pot_id, {'inviteCode': code}, token)
    assert status == 200, (status, body)
wait_for('everyone paid', lambda: sum(1 for m in get_pot()['members'] if m['status'] == 'ACTIVE') == 4)
print('balances after joining (A, B, C, D):', [balance(t) for t in (a_token, b_token, c_token, d_token)])
assert [balance(t) for t in (a_token, b_token, c_token, d_token)] == [400000] * 4

# Dev changes their mind before bidding starts: a full refund
assert call('POST', POT + '/pots/%s/leave' % pot_id, token=a_token)[0] == 422  # the owner cannot leave
assert call('POST', POT + '/pots/%s/leave' % pot_id, token=d_token)[0] == 200
wait_for('the refund', lambda: balance(d_token) == 500000)
print('Dev left and was refunded: balance', balance(d_token))
assert get_pot()['memberCount'] == 3

# the start time comes: joining closes, bidding opens by itself
wait_for('bidding to start', lambda: get_pot()['status'] == 'ACTIVE' and get_pot()['round']['status'] == 'BIDDING', seconds=90)
started = get_pot()
print('bidding started: members', started['memberCount'], 'pot', started['potAmount'], 'lowest bid allowed', started['minBid'])
assert started['memberCount'] == 3 and started['potAmount'] == 300000
assert call('POST', POT + '/pots/%s/join' % pot_id, {'inviteCode': code}, d_token)[0] == 422  # joining is closed
assert call('POST', POT + '/pots/%s/leave' % pot_id, token=b_token)[0] == 422  # and nobody can leave

# round 1
assert call('POST', POT + '/pots/%s/bids' % pot_id, {'amount': 250000}, a_token)[0] == 200
assert call('POST', POT + '/pots/%s/bids' % pot_id, {'amount': 240000}, b_token)[0] == 200
assert call('POST', POT + '/pots/%s/bids' % pot_id, {'amount': 270000}, c_token)[0] == 200
status, body = call('POST', POT + '/pots/%s/bids' % pot_id, {'amount': 100}, c_token)
assert status == 422, (status, body)
print('a bid below the floor is refused:', body.get('detail'))
assert call('POST', POT + '/pots/%s/settle' % pot_id, token=a_token)[0] == 200

wait_for('round 2 open', lambda: get_pot()['currentRound'] == 2 and get_pot()['round']['status'] in ('COLLECTING', 'BIDDING'))
history = get_pot()['history']
print('round 1 history', [(names[h['winnerId']], h['payout']) for h in history])
assert history[0]['winnerId'] == b_id and history[0]['payout'] == 240000

# round 2: B has won and cannot bid again
wait_for('round 2 bidding', lambda: get_pot()['round']['status'] == 'BIDDING')
status, body = call('POST', POT + '/pots/%s/bids' % pot_id, {'amount': 200000}, b_token)
assert status == 422, (status, body)
print('the winner cannot bid again:', body.get('detail'))
assert call('POST', POT + '/pots/%s/bids' % pot_id, {'amount': 280000}, a_token)[0] == 200
assert call('POST', POT + '/pots/%s/bids' % pot_id, {'amount': 190000}, c_token)[0] == 200
assert call('POST', POT + '/pots/%s/settle' % pot_id, token=a_token)[0] == 200

# round 3: A is the last one left and is paid the whole pot with no bidding
wait_for('pot completed', lambda: get_pot()['status'] == 'COMPLETED')
final = get_pot()
print('history', [(names[h['winnerId']], h['payout']) for h in final['history']])
balances = [balance(t) for t in (a_token, b_token, c_token)]
print('final balances A, B, C =', balances, 'sum =', sum(balances))
assert balances == [585000, 495000, 420000], balances
assert sum(balances) == 1500000
assert all(m['won'] for m in final['members'])
assert len(final['history']) == 3

status, ledger = call('GET', WALLET + '/wallet/ledger?page=0&size=20', token=d_token)
print('Dev ledger (newest first):', [(e['kind'], e['type'], e['amount']) for e in ledger['items']])
print('END TO END OK')
