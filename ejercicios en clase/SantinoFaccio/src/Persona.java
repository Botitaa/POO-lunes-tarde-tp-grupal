public class Persona {

    String nombre;
    String apellidos;
    String númeroDocumentoIdentidad;
    int añoNacimiento;
    //EJERCICIO:
    String paisNaciomiento;
    char genero;


    //se invoca cuando se crea el metodo!!!!!
    Persona(String nombre, String apellidos,
            String númeroDocumentoIdentidad, int añoNacimiento, String paisNacimiento,char genero) {
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.númeroDocumentoIdentidad = númeroDocumentoIdentidad;
        this.añoNacimiento = añoNacimiento;
        this.paisNaciomiento = paisNacimiento;
        this.genero = genero; //this hace referencia al atributo del objeto recien creado en este objeto
        //sin el this, significa el valor del parametro nada mas.
    }

    void imprimir() {
        System.out.println("Nombre = " + nombre);
        System.out.println("Apellidos = " + apellidos);
        System.out.println("Número de documento de identidad = " + númeroDocumentoIdentidad);
        System.out.println("Año de nacimiento = " + añoNacimiento);
        System.out.println("Pais nacimiento = " + paisNaciomiento);
        System.out.println("GENERO = " + genero);
    }

    public static void main(String args[]) {
        Persona p1 = new Persona("Pedro", "Pérez", "1053121010", 1998, "Argentina",'h');
        Persona p2 = new Persona("Luis", "León", "1053223344", 2001,"Uruguay",'m');
        p1.imprimir();
        p2.imprimir();
    }
}
