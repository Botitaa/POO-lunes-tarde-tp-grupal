package clases;

public class Persona {
    String nombre;
    String apellidos;
    String númeroDocumentoIdentidad;
    int añoNacimiento;
    String paisDeNacimiento;
    char genero;

    Persona(String nombre, String apellidos,
            String númeroDocumentoIdentidad, int añoNacimiento, String paisDeNacimiento, char genero) {
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.númeroDocumentoIdentidad = númeroDocumentoIdentidad;
        this.añoNacimiento = añoNacimiento;
        this.paisDeNacimiento = paisDeNacimiento;
        this.genero = genero;
    }

    void imprimir() {
        System.out.println("Nombre = " + nombre);
        System.out.println("Apellidos = " + apellidos);
        System.out.println("Número de documento de identidad = " + númeroDocumentoIdentidad);
        System.out.println("Año de nacimiento = " + añoNacimiento);
        System.out.println("Pais de nacimiento: "+ paisDeNacimiento);
        System.out.println("Genero: "+ genero);
        System.out.println();
    }

    public static void main(String args[]) {
        Persona p1 = new Persona("Pedro", "Pérez", "1053121010", 1998, "Argentina", 'H');
        Persona p2 = new Persona("Luis", "León", "1053223344", 2001, "Bolivia", 'M');
        p1.imprimir();
        p2.imprimir();
    }
}
