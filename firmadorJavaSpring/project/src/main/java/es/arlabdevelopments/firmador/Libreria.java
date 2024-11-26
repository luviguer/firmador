package es.arlabdevelopments.firmador;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Enumeration;

public class Libreria {
    /**
     * Metodo para cifrar una string a algoritmo SHA-256
     *
     * @param cadena Cadena de texto a cifrar
     * @return Una cadena de texto con el texto de «cadena» cifrado
     */
    public static String cifrar(String cadena) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] messageDigest = md.digest(cadena.getBytes());
            BigInteger number = new BigInteger(1, messageDigest);
            String hashtext = number.toString(16);

            while (hashtext.length() < 32)
                hashtext = "0".concat(hashtext);

            return hashtext;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Metodo que devuelve el KeyStore de los certificados del sistema, probado
     * principalmente en Windows
     *
     * @return El KeyStore por defecto que contiene los certificados del usuario del
     * sistema
     */
    public static KeyStore certificadosSistema() {
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("win")) {
                return KeyStore.getInstance("Windows-MY");
            } else if (osName.contains("mac")) {
                return KeyStore.getInstance("KeychainStore");
            } else {
                // Para sistemas basados en Unix/Linux, aquí se usa JKS como ejemplo
                return KeyStore.getInstance("JKS");
            }
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Metodo que recibido un KeyStore comprueba los alias que contiene y los
     * devuelve
     *
     * @param ks El KeyStore del que puedes recibir los certificados
     * @return Un ArrayList de Strings que contiene los alias de un almacen
     */
    public static ArrayList<String> comprobarAlias(KeyStore ks) {

        Enumeration<String> enumer;
        ArrayList<String> lista = new ArrayList<String>();

        try {
            ks.load(null, null);

            enumer = ks.aliases();
            while (enumer.hasMoreElements()) {
                String s = enumer.nextElement();
                lista.add(s);
            }

        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Metodo que recibe un fichero, obtiene el certificado y devuelve los alias
     * contenidos en el mismo
     *
     * @param cert Fichero que contiene un certificado PKCS12
     * @return Un array list
     */
    public static ArrayList<String> comprobarAlias(File cert) {
        KeyStore ks = obtenerKeyStore(cert);

        Enumeration<String> enumer;
        ArrayList<String> lista = new ArrayList<String>();

        try {
            enumer = ks.aliases();
            while (enumer.hasMoreElements()) {
                String s = enumer.nextElement();
                lista.add(s);
            }

        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Metodo que devuelve la clave de un fichero dadas las credenciales
     *
     * @param alias      El alias del certificado
     * @param contrasena La contraseña del certificado
     * @param cert       El fichero que referencia al certificado
     * @return Un objeto tipo Key en caso de que el fichero y las credenciales
     * referenciasen a una clave privada, en caso de que la clave no fuese
     * correcta o no referenciase a una clave privada devolvera <b>null</b>
     * LUCIA: AÑADIR QUE HE COMPROBADO QUE LA CONTRASEÑA SEA INCORRECTA 
     */
    public static Key clave(String alias, String contrasena, File cert) {
        Key k = null;
        try (FileInputStream fis = new FileInputStream(cert)) {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(fis, contrasena.toCharArray()); 
            k = ks.getKey(alias, contrasena.toCharArray());
            if (!(k instanceof PrivateKey)) {
                System.err.println("El alias no referencia a una clave privada.");
                return null;
            }
        } catch (IOException e) {
            System.err.println("Error al leer el archivo del certificado: " + e.getMessage());
        } catch (KeyStoreException e) {
            System.err.println("Error con el almacén de claves: " + e.getMessage());
        } catch (NoSuchAlgorithmException | UnrecoverableKeyException e) {
            System.err.println("Error al recuperar la clave: Verifica la contraseña o el alias.");
        } catch (CertificateException e) {
            System.err.println("Certificado inválido: " + e.getMessage());
        }
        return k;
    }

    /**
     * Metodo que obtiene la clave de un certificado del almacen del sistema dadas
     * las credenciales
     *
     * @param alias      El alias del certificado
     * @param contrasena La contraseña del certificado
     * @return Un objeto tipo Key en caso de que el fichero y las credenciales
     * referenciasen a una clave privada, en caso de que la clave no fuese
     * correcta o no referenciase a una clave privada devolvera <b>null</b>
     */
    public static Key clave(String alias, String contrasena) {
        Key k = null;
        try {
            KeyStore ks = certificadosSistema();
            ks.load(null, null);
            k = ks.getKey(alias, contrasena.toCharArray());
            if (!(k instanceof PrivateKey)) return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return k;
    }



    /**
     * Metodo para obtener una KeyStore recibiendo un fichero
     *
     * @param cert El fichero que contiene el certificado
     * @return La KeyStore contenida en el certificado
     */
    private static KeyStore obtenerKeyStore(File cert) {
        KeyStore ks = null;
        try (FileInputStream fis = new FileInputStream(cert)) {
            ks = KeyStore.getInstance("PKCS12");
            ks.load(fis, null);
        } catch (FileNotFoundException ex) {

        } catch (IOException ex) {

        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException ex) {
            System.err.println("Problemas con el certificado");
        }
        return ks;
    }
}