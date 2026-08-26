public class SantinoFaccioEjercicios5Arrays {

    public static void main(String[] args){
        int[] array = new int[]{22,33,44,2,67,99};
        System.out.println("el elemento mayor del array es: " + elemMayor(array) + " y el menor es: " + elemMinimo(array));
        System.out.println(busqueda(array,23));
        System.out.println(busqueda(array,22));
        int[] array2 = new int[]{77,22,99,1,10};
        BuscarEnComun(array,array2);
    }

    private static void BuscarEnComun(int[] array, int[] array2){
        for(int i = 0; i < array.length; i++ ){
            for(int i2 = 0; i2 < array2.length;i2++){
                if(array[i] == array2[i2]){
                    System.out.println(array[i]);
                }
            }
        }
    }

    private static int elemMayor(int[] array){
        int max = array[0];
        for (int i = 1; i < array.length; i++){
            if (array[i] > max){
                max = array[i];
            }
        }
        return max;
    }
    private static int elemMinimo(int[] array){
        int min = array[0];
        for (int i = 1; i < array.length;i++){
            if (array[i] < min){
                min = array[i];
            }
        }
        return min;
    }
    private static boolean busqueda(int[] array,int busqueda){
        for(int i = 0; i < array.length; i++){
            if(array[i] == busqueda){
                return true;
            }
        }
        return false;
    }
}
