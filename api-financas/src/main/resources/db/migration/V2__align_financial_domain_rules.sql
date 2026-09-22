-- A movimentacao representa dinheiro realizado. Rendas e gastos recorrentes
-- sao modelos de planejamento e so afetam o saldo ao originar uma movimentacao.

ALTER TABLE tb_movimentacoes
    ADD CONSTRAINT chk_movimentacoes_valor_positivo CHECK (valor > 0);

DROP INDEX IF EXISTS idx_movimentacoes_usuario_id;
DROP INDEX IF EXISTS idx_movimentacoes_data_movimentacao;
CREATE INDEX idx_movimentacoes_usuario_data
    ON tb_movimentacoes (usuario_id, data_movimentacao DESC);

ALTER TABLE tb_rendas
    ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE,
    ADD CONSTRAINT chk_rendas_valor_positivo CHECK (valor_base > 0);

ALTER TABLE tb_cartoes_credito
    ADD CONSTRAINT chk_cartoes_limite_nao_negativo CHECK (limite >= 0);

UPDATE tb_dividas SET parcelas_pagas = 0 WHERE parcelas_pagas IS NULL;
ALTER TABLE tb_dividas
    ALTER COLUMN parcelas_pagas SET DEFAULT 0,
    ALTER COLUMN parcelas_pagas SET NOT NULL,
    ADD CONSTRAINT chk_dividas_valor_positivo CHECK (valor_total > 0),
    ADD CONSTRAINT chk_dividas_total_parcelas CHECK (total_parcelas IS NULL OR total_parcelas > 0),
    ADD CONSTRAINT chk_dividas_status CHECK (status IN ('ABERTA', 'QUITADA', 'ATRASADA')),
    ADD CONSTRAINT chk_dividas_parcelas_pagas CHECK (
        parcelas_pagas >= 0
        AND (total_parcelas IS NULL OR parcelas_pagas <= total_parcelas)
    );

ALTER TABLE tb_gastos_recorrentes
    ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE,
    ADD CONSTRAINT chk_gastos_valor_positivo CHECK (valor > 0);
