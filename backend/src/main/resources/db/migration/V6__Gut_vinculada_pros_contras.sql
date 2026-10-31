-- A Matriz GUT passa a classificar os próprios prós e contras da decisão.
-- Cada pró/contra tem no máximo uma classificação; excluir o pró/contra exclui a classificação.
-- Fatores antigos, digitados à mão, continuam válidos com pro_contra_id nulo.
ALTER TABLE itens_gut
    ADD COLUMN pro_contra_id BIGINT NULL,
    ADD CONSTRAINT uk_itens_gut_pro_contra_id UNIQUE (pro_contra_id),
    ADD CONSTRAINT fk_itens_gut_pro_contra_id FOREIGN KEY (pro_contra_id) REFERENCES pros_contras (id) ON DELETE CASCADE;
