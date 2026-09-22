export interface LoginRequest {
  email: string;
  senha: string;
}

export interface RegisterRequest {
  nome: string;
  email: string;
  senha: string;
}

export interface UserResponse {
  id: string;
  nome: string;
  email: string;
  dataCriacao: string;
}

export interface ApiError {
  mensagem?: string;
  campos?: Record<string, string>;
}
