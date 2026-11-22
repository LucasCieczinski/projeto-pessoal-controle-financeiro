export type DebtStatus = 'ABERTA' | 'QUITADA' | 'ATRASADA';

export interface Debt {
  id: string;
  credor: string;
  valorTotal: number;
  totalParcelas: number | null;
  parcelasPagas: number;
  status: DebtStatus;
}

export interface DebtRequest {
  credor: string;
  valorTotal: number;
  totalParcelas: number | null;
  parcelasPagas: number;
  status: DebtStatus;
}
