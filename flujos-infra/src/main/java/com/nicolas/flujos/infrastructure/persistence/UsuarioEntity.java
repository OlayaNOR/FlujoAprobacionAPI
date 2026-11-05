package com.nicolas.flujos.infrastructure.persistence;

import com.nicolas.flujos.domain.flujo.RolAprobador;
import com.nicolas.flujos.domain.usuario.Rol;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "usuario")
class UsuarioEntity {

    @Id
    UUID id;

    String email;

    String nombre;

    @Column(name = "password_hash")
    String passwordHash;

    boolean activo;

    @Enumerated(EnumType.STRING)
    RolAprobador cargo;

    @Column(name = "jefe_id")
    UUID jefeId;

    @Column(name = "creado_en")
    Instant creadoEn;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_rol", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "rol")
    @Enumerated(EnumType.STRING)
    Set<Rol> roles;

    protected UsuarioEntity() {
    }
}
