package com.grupo06sa.sistema_inventario.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.grupo06sa.sistema_inventario.repository.AlmacenRepository;
import com.grupo06sa.sistema_inventario.repository.CategoriaRepository;
import com.grupo06sa.sistema_inventario.repository.ProductoRepository;
import com.grupo06sa.sistema_inventario.repository.ProveedorRepository;
import com.grupo06sa.sistema_inventario.repository.RolRepository;
import com.grupo06sa.sistema_inventario.repository.TipoOperacionRepository;
import com.grupo06sa.sistema_inventario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class DatabaseSeederConditionTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(DatabaseSeeder.class, RepositoryConfig.class);

    @Test
    void noCargaSeederCuandoEstaDesactivado() {
        contextRunner
            .withPropertyValues("app.seed.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(DatabaseSeeder.class));
    }

    @Test
    void cargaSeederCuandoEstaActivado() {
        contextRunner
            .withPropertyValues("app.seed.enabled=true")
            .run(context -> assertThat(context).hasSingleBean(DatabaseSeeder.class));
    }

    @Configuration
    static class RepositoryConfig {
        @Bean
        RolRepository rolRepository() {
            return mock(RolRepository.class);
        }

        @Bean
        TipoOperacionRepository tipoOperacionRepository() {
            return mock(TipoOperacionRepository.class);
        }

        @Bean
        CategoriaRepository categoriaRepository() {
            return mock(CategoriaRepository.class);
        }

        @Bean
        ProveedorRepository proveedorRepository() {
            return mock(ProveedorRepository.class);
        }

        @Bean
        AlmacenRepository almacenRepository() {
            return mock(AlmacenRepository.class);
        }

        @Bean
        UsuarioRepository usuarioRepository() {
            return mock(UsuarioRepository.class);
        }

        @Bean
        ProductoRepository productoRepository() {
            return mock(ProductoRepository.class);
        }
    }
}
