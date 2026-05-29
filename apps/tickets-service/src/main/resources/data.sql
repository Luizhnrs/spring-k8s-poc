-- Initialize ticket counters with zero for all known types
INSERT INTO tickets (type, count, created_at, updated_at)
SELECT 'NOTIFICACAO', 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM tickets WHERE type = 'NOTIFICACAO');

INSERT INTO tickets (type, count, created_at, updated_at)
SELECT 'AUDITORIA', 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM tickets WHERE type = 'AUDITORIA');

INSERT INTO tickets (type, count, created_at, updated_at)
SELECT 'PRIORIDADE', 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM tickets WHERE type = 'PRIORIDADE');

INSERT INTO tickets (type, count, created_at, updated_at)
SELECT 'METRICAS', 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM tickets WHERE type = 'METRICAS');