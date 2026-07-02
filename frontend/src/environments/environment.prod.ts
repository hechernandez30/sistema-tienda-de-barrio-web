// Entorno de PRODUCCIÓN (build usada por Docker / Nginx).
// La API se consume por ruta relativa "/api": Nginx hace de proxy hacia el backend.
// Esto permite acceder desde cualquier PC de la red usando la IP del servidor,
// sin depender de "localhost" ni de una IP fija codificada.
export const environment = {
  production: true,
  apiUrl: '/api',
};
