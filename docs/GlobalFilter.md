Este GlobalFilter garantiza que todas las peticiones que atraviesan el Gateway tengan un identificador único:

X-Request-Id

Funciona así:

Si el cliente YA manda un X-Request-Id
→ Lo respeta.

Si NO lo manda
→ Genera uno (UUID).

Lo añade a:

La request hacia los microservicios.

La response hacia el cliente.

Resultado:

Cada request tiene un ID único y trazable end-to-end.