/** Money is in paise. */
export type PotStatus = 'OPEN' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
export type MemberStatus = 'PENDING' | 'ACTIVE' | 'LEFT';
export type RoundStatus = 'COLLECTING' | 'BIDDING' | 'SETTLING' | 'SETTLED';
export type TransferKind = 'CONTRIBUTION' | 'PAYOUT' | 'DIVIDEND' | 'REFUND';
export type TransferStatus = 'REQUESTED' | 'DONE' | 'REJECTED' | 'VOID';

export interface PotMemberResponse {
  userId: string;
  joinOrder: number;
  /** PENDING while the member's contribution is on its way from the wallet. */
  status: MemberStatus;
  won: boolean;
  wonRound: number | null;
}

export interface RoundTransferResponse {
  userId: string;
  kind: TransferKind;
  amount: number;
  status: TransferStatus;
  reason: string | null;
}

export interface BidResponse {
  userId: string;
  amount: number;
  placedAt: string;
}

export interface RoundResponse {
  id: string;
  number: number;
  status: RoundStatus;
  biddingEndsAt: string | null;
  winnerId: string | null;
  payout: number | null;
  /** Only for members of the pot. */
  transfers: RoundTransferResponse[];
  /** Only for members of the pot, lowest first. */
  bids: BidResponse[];
}

export interface PastRoundResponse {
  number: number;
  winnerId: string;
  payout: number | null;
}

export interface PotResponse {
  id: string;
  name: string;
  ownerId: string;
  status: PotStatus;
  /** The members who have paid (before bidding starts), or the group that started. */
  memberCount: number;
  contribution: number;
  /** What a round pays out: every member contributes once. */
  potAmount: number;
  minBid: number;
  minBidPercent: number;
  bidWindowSeconds: number;
  currentRound: number;
  /** When joining closes and bidding begins. */
  startsAt: string;
  /** What a member needs to join. Only the owner gets it; for everyone else it is null. */
  inviteCode: string | null;
  /** Why the viewer is not in, when their payment was refused (for example "Insufficient funds"). */
  joinProblem: string | null;
  members: PotMemberResponse[];
  /** Null until the pot starts. */
  round: RoundResponse | null;
  history: PastRoundResponse[];
}

export interface PotSummaryResponse {
  id: string;
  name: string;
  status: PotStatus;
  memberCount: number;
  contribution: number;
  potAmount: number;
  currentRound: number;
  startsAt: string;
}

export interface PotListResponse {
  items: PotSummaryResponse[];
}

/** Open `/api/ws/pots?ticket=<ticket>` on the pot service within 30 seconds; it works once. */
export interface SocketTicketResponse {
  ticket: string;
}
