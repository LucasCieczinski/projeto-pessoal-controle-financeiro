export interface Income {
  id: string;
  fonte: string;
  valorBase: number;
  ativo: boolean;
}

export interface IncomeRequest {
  fonte: string;
  valorBase: number;
  ativo: boolean;
}

export interface RecurringExpense {
  id: string;
  descricao: string;
  valor: number;
  diaVencimento: number;
  ativo: boolean;
}

export interface RecurringExpenseRequest {
  descricao: string;
  valor: number;
  diaVencimento: number;
  ativo: boolean;
}
