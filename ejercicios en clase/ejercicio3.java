public class ejercicio3 {
    public static void main(String[] args){
        int anio = 2024;
        if (anio % 400 == 0) {
            System.out.println(anio + ": Es bisiesto");
        } else if (anio % 100 == 0) {
            System.out.println(anio + ": No es bisiesto");
        } else if (anio % 4 == 0) {
            System.out.println(anio + ": Es bisiesto");
        } else {
            System.out.println(anio + ": No es bisiesto");
        }
    }
}
