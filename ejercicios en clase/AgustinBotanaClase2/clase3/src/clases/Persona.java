package clases;

public class Persona {
    String nombre;
    String apellidos;
    String númeroDocumentoIdentidad;
    int añoNacimiento;

    Persona(String nombre, String apellidos,
            String númeroDocumentoIdentidad, int añoNacimiento) {
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.númeroDocumentoIdentidad = númeroDocumentoIdentidad;
        this.añoNacimiento = añoNacimiento;
    }

    void imprimir() {
        System.out.println("Nombre = " + nombre);
        System.out.println("Apellidos = " + apellidos);
        System.out.println("Número de documento de identidad = " + númeroDocumentoIdentidad);
        System.out.println("Año de nacimiento = " + añoNacimiento);
        System.out.println();
    }

    public static void main(String args[]) {
        Persona p1 = new Persona("Pedro", "Pérez", "1053121010", 1998);
        Persona p2 = new Persona("Luis", "León", "1053223344", 2001);
        p1.imprimir();
        p2.imprimir();
    }
}
