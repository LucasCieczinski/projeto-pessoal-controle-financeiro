export type MovementType = 'ENTRADA' | 'SAIDA';

export interface MovementSummary {
  mes: string;
  entradas: number;
  saidas: number;
  resultado: number;
  saldoAnterior: number;
  saldoAcumulado: number;
  quantidade: number;
  totalRegistros: number;
  ultimasMovimentacoes: Movement[];
}

export interface Movement {
  id: string;
  descricao: string;
  valor: number;
  tipo: MovementType;
  categoria: string;
  dataMovimentacao: string;
  dataCadastro: string;
}

export interface CreateMovementRequest {
  descricao: string;
  valor: number;
  tipo: MovementType;
  categoria: string;
  dataMovimentacao: string;
}
