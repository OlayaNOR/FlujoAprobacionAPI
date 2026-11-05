package com.nicolas.flujos.infrastructure.messaging;

/** El destino de los eventos. Cambiarlo por una cola no toca nada fuera de este paquete. */
public interface PublicadorEventos {

    void publicar(EventoPendiente evento);
}
