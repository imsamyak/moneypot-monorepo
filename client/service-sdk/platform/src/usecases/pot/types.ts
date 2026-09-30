/** Money is in paise. */
export type PotStatus = 'OPEN' | 'ACTIVE' | 'COMPLETED';
export type RoundStatus = 'COLLECTING' | 'BIDDING' | 'SETTLING' | 'SETTLED';
export type TransferKind = 'CONTRIBUTION' | 'PAYOUT' | 'DIVIDEND';
export type TransferStatus = 'REQUESTED' | 'DONE' | 'REJECTED';

export interface PotMemberResponse {
  userId: string;
  joinOrder: number;
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
  memberCount: number;
  contribution: number;
  potAmount: number;
  minBid: number;
  minBidPercent: number;
  bidWindowSeconds: number;
  currentRound: number;
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
  joined: number;
  contribution: number;
  potAmount: number;
  currentRound: number;
}

export interface PotListResponse {
  items: PotSummaryResponse[];
}

/** Open `/api/ws/pots?ticket=<ticket>` on the pot service within 30 seconds; it works once. */
export interface SocketTicketResponse {
  ticket: string;
}
