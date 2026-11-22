export interface CreditCard {
  id: string;
  nomeCartao: string;
  limite: number;
  diaFechamento: number;
  diaVencimento: number;
}

export interface CreditCardRequest {
  nomeCartao: string;
  limite: number;
  diaFechamento: number;
  diaVencimento: number;
}
