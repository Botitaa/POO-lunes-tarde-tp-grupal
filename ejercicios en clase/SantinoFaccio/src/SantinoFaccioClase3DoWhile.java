public class SantinoFaccioClase3DoWhile {


    public static void main(String[] args) {
        System.out.println(Ejercicio1(1000));
        int[] array = {10,2,4,1,9};
        System.out.println(Ejercicio2(array));
    }
    public static int Ejercicio1(int num){
        int cifras = 0;
        do{
            num = num / 10;
            cifras++;
        }
        while(num != 0);

        return cifras;
    }
    public static int Ejercicio2(int[] notas){
        int i = 0;
        int contador = 0;
        do {
            if(notas[i] >= 3){
                contador++;
            }
            i++;
        }
         while(i < notas.length);
    return contador;
    }
}
