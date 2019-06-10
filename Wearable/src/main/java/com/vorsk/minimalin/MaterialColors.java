package com.vorsk.minimalin;

public class MaterialColors {

    public static final int A100 = 10;
    public static final int A200 = 11;
    public static final int A400 = 12;
    public static final int A700 = 13;
    private static final int default_weight = 500;

    // palettes for each color family
    // https://github.com/hyunbin/MaterialColors/blob/master/src/me/hyunbin/colors/Palettes.java
    // 50 100 200 300 400 500 600 700 800 900 (A100 A200 A400 A700)
    private static final int[] redPalette = {0xFFEBEE, 0xFFCD02, 0xEF9A9A, 0xE57373, 0xEF5350, 0xF44336, 0xE53935, 0xD32F2F, 0xC62828, 0xB71C1C, 0xFF8A80, 0xFF5252, 0xFF1744, 0xD50000};
    private static final int[] pinkPalette = {0xFCE4EC, 0xF8BBD0, 0xF48FB1, 0xF06292, 0xEC407A, 0xE91E63, 0xD81B60, 0xC2185B, 0xAD1457, 0x880E4F, 0xFF80AB, 0xFF4081, 0xF50057, 0xC51162};
    private static final int[] purplePalette = {0xF3E5F5, 0xE1BEE7, 0xCE93D8, 0xBA68C8, 0xAB47BC, 0x9C27B0, 0x8E24AA, 0x7B1FA2, 0x6A1B9A, 0x4A148C, 0xEA80FC, 0xE040FB, 0xD500F9, 0xAA00FF};
    private static final int[] deepPurplePalette = {0xEDE7F6, 0xD1C4E9, 0xB39DDB, 0x9575CD, 0x7E57C2, 0x673AB7, 0x5E35B1, 0x512DA8, 0x4527A0, 0x311B92, 0xB388FF, 0x7C4DFF, 0x651FFF, 0x6200EA};
    private static final int[] indigoPalette = {0xE8EAF6, 0xC5CAE9, 0x9FA8DA, 0x7986CB, 0x5C6BC0, 0x3F51B5, 0x3949AB, 0x303F9F, 0x283593, 0x1A237E, 0x8C9EFF, 0x536DFE, 0x3D5AFE, 0x304FFE};
    private static final int[] bluePalette = {0xE3F2FD, 0xBBDEFB, 0x90CAF9, 0x64B5F6, 0x42A5F5, 0x2196F3, 0x1E88E5, 0x1976D2, 0x1565C0, 0x0D47A1, 0x82B1FF, 0x448AFF, 0x2979FF, 0x2962FF};
    private static final int[] lightBluePalette = {0xE1F5FE, 0xB3E5FC, 0x81D4fA, 0x4fC3F7, 0x29B6FC, 0x03A9F4, 0x039BE5, 0x0288D1, 0x0277BD, 0x01579B, 0x80D8FF, 0x40C4FF, 0x00B0FF, 0x0091EA};
    private static final int[] cyanPalette = {0xE0F7FA, 0xB2EBF2, 0x80DEEA, 0x4DD0E1, 0x26C6DA, 0x00BCD4, 0x00ACC1, 0x0097A7, 0x00838F, 0x006064, 0x84FFFF, 0x18FFFF, 0x00E5FF, 0x00B8D4};
    private static final int[] tealPalette = {0xE0F2F1, 0xB2DFDB, 0x80CBC4, 0x4DB6AC, 0x26A69A, 0x009688, 0x00897B, 0x00796B, 0x00695C, 0x004D40, 0xA7FFEB, 0x64FFDA, 0x1DE9B6, 0x00BFA5};
    private static final int[] greenPalette = {0xE8F5E9, 0xC8E6C9, 0xA5D6A7, 0x81C784, 0x66BB6A, 0x4CAF50, 0x43A047, 0x388E3C, 0x2E7D32, 0x1B5E20, 0xB9F6CA, 0x69F0AE, 0x00E676, 0x00C853};
    private static final int[] blueGreyPalette = {0xECEFF1, 0xCFD8DC, 0xB0BBC5, 0x90A4AE, 0x78909C, 0x607D8B, 0x546E7A, 0x455A64, 0x37474F, 0x263238};
    private static final int[] greyPalette = {0xFAFAFA, 0xF5F5F5, 0xEEEEEE, 0xE0E0E0, 0xBDBDBD, 0x9E9E9E, 0x757575, 0x616161, 0x424242, 0x212121, 0x000000, 0xffffff};
    private static final int[] brownPalette = {0xEFEBE9, 0xD7CCC8, 0xBCAAA4, 0xA1887F, 0x8D6E63, 0x795548, 0x6D4C41, 0x5D4037, 0x4E342E, 0x3E2723 };
    private static final int[] deepOrangePalette = {0xFBE9A7, 0xFFCCBC, 0xFFAB91, 0xFF8A65, 0xFF7043, 0xFF5722, 0xF4511E, 0xE64A19, 0xD84315, 0xBF360C, 0xFF9E80, 0xFF6E40, 0xFF3D00, 0xDD2600};
    private static final int[] orangePalette = {0xFFF3E0, 0xFFE0B2, 0xFFCC80, 0xFFB74D, 0xFFA726, 0xFF9800, 0xFB8C00, 0xF57C00, 0xEF6C00, 0xE65100, 0xFFD180, 0xFFAB40, 0xFF9100, 0xFF6D00};
    private static final int[] amberPalette = {0xFFF8E1, 0xFFECB3, 0xFFE082, 0xFFD54F, 0xFFCA28, 0xFFC107, 0xFFB300, 0xFFA000, 0xFF8F00, 0xFF6F00, 0xFFE57F, 0xFFD740, 0xFFC400, 0xFFAB00};
    private static final int[] yellowPalette = {0xFFFDE7, 0xFFF9C4, 0xFFF590, 0xFFF176, 0xFFEE58, 0xFFEB3B, 0xFDD835, 0xFBC02D, 0xF9A825, 0xF57F17, 0xFFFF82, 0xFFFF00, 0xFFEA00, 0xFFD600};
    private static final int[] limePalette = {0xF9FBE7, 0xF0F4C3, 0xE6EE9C, 0xDCE775, 0xD4E157, 0xCDDC39, 0xC0CA33, 0xA4B42B, 0x9E9D24, 0x827717, 0xF4FF81, 0xEEFF41, 0xC6FF00, 0xAEEA00};
    private static final int[] lightGreenPalette = {0xF1F8E9, 0xDCEDC8, 0xC5E1A5, 0xAED581, 0x9CCC65, 0x8BC34A, 0x7CB342, 0x689F38, 0x558B2F, 0x33691E, 0xCCFF90, 0xB2FF59, 0x76FF03, 0x64DD17};
    private static final int[] blackPalette = {0x000000};

    public enum Color
    {
        RED,
        PINK,
        PURPLE,
        DEEP_PURPLE,
        INDIGO,
        BLUE,
        LIGHT_BLUE,
        CYAN,
        TEAL,
        GREEN,
        BLUE_GRAY,
        GREY,
        BROWN,
        DEEP_ORANGE,
        ORANGE,
        AMBER,
        YELLOW,
        LIME,
        LIGHT_GREEN,
        BLACK;

        private static int getValueOffset(int weight) {
            if (weight < 50) {
                return  weight;
            }
            if (weight <= 900) {
                return weight/100;
            }
            return default_weight/500;
        }

        private int[] getColors() {
            switch(this){
                case RED:
                    return redPalette;
                case PINK:
                    return pinkPalette;
                case PURPLE:
                    return purplePalette;
                case DEEP_PURPLE:
                    return deepPurplePalette;
                case INDIGO:
                    return indigoPalette;
                case BLUE:
                    return bluePalette;
                case LIGHT_BLUE:
                    return lightBluePalette;
                case CYAN:
                    return cyanPalette;
                case TEAL:
                    return tealPalette;
                case GREEN:
                    return greenPalette;
                case BLUE_GRAY:
                    return blueGreyPalette;
                case GREY:
                    return greyPalette;
                case BROWN:
                    return brownPalette;
                case DEEP_ORANGE:
                    return deepOrangePalette;
                case ORANGE:
                    return orangePalette;
                case AMBER:
                    return amberPalette;
                case YELLOW:
                    return yellowPalette;
                case LIME:
                    return limePalette;
                case LIGHT_GREEN:
                    return lightGreenPalette;
                case BLACK:
                    return blackPalette;
            }
            // should never happen
            return null;
        }

        public String NiceName() {
            switch(this){
                case RED:
                    return "Red";
                case PINK:
                    return "Pink";
                case PURPLE:
                    return "Purple";
                case DEEP_PURPLE:
                    return "Deep Purple";
                case INDIGO:
                    return "Indigo";
                case BLUE:
                    return "Blue";
                case LIGHT_BLUE:
                    return "Light Blue";
                case CYAN:
                    return "Cyan";
                case TEAL:
                    return "Teal";
                case GREEN:
                    return "Green";
                case BLUE_GRAY:
                    return "BlueGrey";
                case GREY:
                    return "Grey";
                case BROWN:
                    return "Brown";
                case DEEP_ORANGE:
                    return "Deep Orange";
                case ORANGE:
                    return "Orange";
                case AMBER:
                    return "Amber";
                case YELLOW:
                    return "Yellow";
                case LIME:
                    return "Lime";
                case LIGHT_GREEN:
                    return "Light Green";
                case BLACK:
                    return "Black";
            }
            // should never happen
            return "???";
        }

        public int Color() {
            return this.Color(default_weight);
        }

        public int Color(int weight) {
            int[] colors = this.getColors();
            int offset = getValueOffset(weight);
            if (offset >= colors.length) {
                // offset out of range
                //return 0xFF0808; // red/grey
                offset = 0;
            }
            return colors[offset] | 0xFF000000; // set alpha
        }

    }

    // from: https://stackoverflow.com/questions/24260853/check-if-color-is-dark-or-light-in-android/24261119
    // use for determining if the text on this color should be black or white
    public static boolean isColorDark(int color){
        double darkness = 1-(0.299* android.graphics.Color.red(color) + 0.587* android.graphics.Color.green(color) + 0.114* android.graphics.Color.blue(color))/255;
        if(darkness<0.5){
            return false; // It's a light color
        }else{
            return true; // It's a dark color
        }
    }

    public static Color[] Colors() {
        return Color.values();
    }

    public static int[] AllColors() {
        // TODO optimize & debug this
        Color[] baseColors = Color.values();
        int[] colors = new int[0];
        for (Color base : baseColors) {
            colors = concatenate(colors, base.getColors());
        }
        return colors;
    }

    public static Color Get(String name) {
        return Color.valueOf(name);
    }

    private static int[] concatenate(int[] a, int[] b) {
        int aLen = a.length;
        int bLen = b.length;

        int[] c = new int[aLen + bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);

        return c;
    }
}
