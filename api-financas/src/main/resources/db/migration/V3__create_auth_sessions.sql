CREATE TABLE tb_sessoes_auth (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL REFERENCES tb_usuarios (id) ON DELETE CASCADE,
    refresh_token_hash VARCHAR(64) NOT NULL UNIQUE,
    expira_em TIMESTAMP WITH TIME ZONE NOT NULL,
    criada_em TIMESTAMP WITH TIME ZONE NOT NULL,
    persistente BOOLEAN NOT NULL
);

CREATE INDEX idx_sessoes_auth_usuario_id ON tb_sessoes_auth (usuario_id);
CREATE INDEX idx_sessoes_auth_expira_em ON tb_sessoes_auth (expira_em);
