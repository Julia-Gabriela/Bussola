-- Marca quando a decisão em andamento exibiu o Resultado Comparativo pela primeira vez.
-- Não encerra a decisão: o status só muda ao finalizar. Linhas existentes ficam com NULL.
ALTER TABLE decisoes ADD COLUMN resultado_alcancado_em DATETIME(6) NULL;
