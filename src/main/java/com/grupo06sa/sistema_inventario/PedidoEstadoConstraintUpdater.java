package com.grupo06sa.sistema_inventario;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class PedidoEstadoConstraintUpdater implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(PedidoEstadoConstraintUpdater.class);

    private final DataSource dataSource;

    public PedidoEstadoConstraintUpdater(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        updatePedidoConstraint();
        updateCompraConstraint();
    }

    private void updatePedidoConstraint() {
        if (!tableExists("pedido")) {
            logger.warn("Pedido table not found; skipping estado constraint update.");
            return;
        }

        String dropSql = "ALTER TABLE pedido DROP CONSTRAINT IF EXISTS pedido_estado_check";
        String addSql = "ALTER TABLE pedido ADD CONSTRAINT pedido_estado_check "
            + "CHECK (estado IN ('PENDIENTE','EN_ESPERA','ESPERANDO_PAGO','PAGADO','ENTREGADO','CANCELADO'))";

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(dropSql);
            statement.execute(addSql);
            logger.info("Pedido estado constraint updated.");
        } catch (Exception ex) {
            logger.warn("Failed to update pedido_estado_check constraint", ex);
        }
    }

    private void updateCompraConstraint() {
        if (!tableExists("compra")) {
            logger.warn("Compra table not found; skipping estado constraint update.");
            return;
        }

        String dropSql = "ALTER TABLE compra DROP CONSTRAINT IF EXISTS compra_estado_check";
        String addSql = "ALTER TABLE compra ADD CONSTRAINT compra_estado_check "
            + "CHECK (estado IN ('PENDIENTE','EN_ESPERA','ESPERANDO_PAGO','PAGADO','ENTREGADO','CANCELADO'))";

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(dropSql);
            statement.execute(addSql);
            logger.info("Compra estado constraint updated.");
        } catch (Exception ex) {
            logger.warn("Failed to update compra_estado_check constraint", ex);
        }
    }

    private boolean tableExists(String tableName) {
        String sql = "SELECT 1 FROM information_schema.tables WHERE table_name = '" + tableName + "'";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSet.next();
        } catch (Exception ex) {
            logger.warn("Failed to check table existence for {}", tableName, ex);
            return false;
        }
    }
}
