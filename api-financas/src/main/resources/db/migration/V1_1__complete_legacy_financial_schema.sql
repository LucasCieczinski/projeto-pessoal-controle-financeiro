-- Corrige bancos antigos que continham apenas tb_usuarios, mas foram
-- registrados pelo Flyway como baseline 1 antes da criacao das demais tabelas.
-- Em bancos vazios, V1 ja criou tudo e esta migracao nao recria objetos.
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

ALTER TABLE tb_usuarios ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE tb_usuarios ALTER COLUMN data_criacao SET DEFAULT NOW();

CREATE TABLE IF NOT EXISTS tb_movimentacoes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES tb_usuarios (id) ON DELETE CASCADE,
    descricao VARCHAR(255) NOT NULL,
    valor NUMERIC(12, 2) NOT NULL,
    tipo VARCHAR(10) NOT NULL CHECK (tipo IN ('ENTRADA', 'SAIDA')),
    categoria VARCHAR(50) NOT NULL,
    data_movimentacao DATE NOT NULL,
    data_cadastro TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_movimentacoes_usuario_id ON tb_movimentacoes (usuario_id);
CREATE INDEX IF NOT EXISTS idx_movimentacoes_data_movimentacao ON tb_movimentacoes (data_movimentacao);

CREATE TABLE IF NOT EXISTS tb_rendas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES tb_usuarios (id) ON DELETE CASCADE,
    fonte VARCHAR(100) NOT NULL,
    valor_base NUMERIC(12, 2) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_rendas_usuario_id ON tb_rendas (usuario_id);

CREATE TABLE IF NOT EXISTS tb_cartoes_credito (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES tb_usuarios (id) ON DELETE CASCADE,
    nome_cartao VARCHAR(50) NOT NULL,
    limite NUMERIC(12, 2) NOT NULL,
    dia_fechamento INTEGER NOT NULL CHECK (dia_fechamento BETWEEN 1 AND 31),
    dia_vencimento INTEGER NOT NULL CHECK (dia_vencimento BETWEEN 1 AND 31)
);

CREATE INDEX IF NOT EXISTS idx_cartoes_credito_usuario_id ON tb_cartoes_credito (usuario_id);

CREATE TABLE IF NOT EXISTS tb_dividas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES tb_usuarios (id) ON DELETE CASCADE,
    credor VARCHAR(100) NOT NULL,
    valor_total NUMERIC(12, 2) NOT NULL,
    total_parcelas INTEGER,
    parcelas_pagas INTEGER,
    status VARCHAR(20) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_dividas_usuario_id ON tb_dividas (usuario_id);

CREATE TABLE IF NOT EXISTS tb_gastos_recorrentes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES tb_usuarios (id) ON DELETE CASCADE,
    descricao VARCHAR(150) NOT NULL,
    valor NUMERIC(12, 2) NOT NULL,
    dia_vencimento INTEGER NOT NULL,
    CONSTRAINT chk_gastos_dia_vencimento CHECK (dia_vencimento BETWEEN 1 AND 31)
);

CREATE INDEX IF NOT EXISTS idx_gastos_recorrentes_usuario_id
    ON tb_gastos_recorrentes (usuario_id);
