/** Money is in paise. */
export interface AccountResponse {
  accountId: string;
  balance: number;
}

export interface LedgerEntryResponse {
  id: string;
  type: 'CREDIT' | 'DEBIT';
  kind: 'DEPOSIT' | 'CONTRIBUTION' | 'PAYOUT' | 'DIVIDEND';
  amount: number;
  balanceAfter: number;
  createdAt: string;
}

export interface LedgerPageResponse {
  items: LedgerEntryResponse[];
  /** The page to ask for next, or null on the last one. */
  nextPage: number | null;
}
