// La URL del backend se resuelve usando el MISMO host desde el que se abrió el
// frontend. Así funciona igual en la máquina local (localhost) que desde otras
// computadoras de la red local (por ejemplo http://192.168.1.50:4200), sin tener
// que codificar una IP fija. Se asume que el backend corre en el puerto 8080 del
// mismo servidor donde se sirve el frontend.
const apiHost = typeof window !== 'undefined' ? window.location.hostname : 'localhost';

export const environment = {
  production: false,
  apiUrl: `http://${apiHost}:8080/api`,
};
