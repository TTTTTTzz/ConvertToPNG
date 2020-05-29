import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Main {
    //路径
    private final static String mapPath = "./map.png";
    private final static String maskPath = "./Mask.txt";
    private final static String configPath = "./DownloadConfiguration.txt";

    private static String asciiDirectory = "";
    private static String pngDirectory = "";

    private static double[][] maskArray;
    //图片的尺寸
    private final static int height = 2000;
    private final static int width = 1300;


    public static void main(String[] args) {

        // 根据坐标生成掩膜文件
        // generateMask();
        // 读取配置文件，设置Ascii文件和png文件的路径
        setDirectory();
        // 读取掩膜文件，转换为数组
        if(!new File(maskPath).exists()){
            Exception e = new Exception("当前目录下找不到掩膜文件Mask.txt！");
            e.printStackTrace();
            printError(e);
            System.exit(1);
        }
        maskArray = readAscii(maskPath);
        // 读取地图，将地图转换为rgb数组
        int[][] mapRgb = readMapRgb(mapPath);
        // 对目录中的每一个Ascii文件进行生成png图片的操作
        File[] asciiList = new File(asciiDirectory).listFiles();

        if (asciiList != null) {
            System.out.println("开始转换...");
            for (File currentFile : asciiList) {
                String filePath = currentFile.getPath();
                // 读取Ascii文件，转换为数组
                double[][] asciiArray = readAscii(filePath);
                //根据Ascii文件的数据，获得降雨量的rgb值
                int[][] rgbArray = getRainFallRgb(asciiArray);
                // 输出图片到指定文件
                String fileName = currentFile.getName();
                convert(pngDirectory + "\\" + fileName.substring(0, fileName.indexOf('.')) + ".png", rgbArray, mapRgb);
            }
        } else {
            printError(new Exception("ASCII数据文件为空！"));
        }

        System.out.println("图片输出完毕!");
    }

    private static void generateMask() {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(maskPath, true), "UTF-8"));
            writer.write("ncols 1300\n" +
                    "nrows 2000\n" +
                    "xllcorner 95.0050\n" +
                    "yllcorner 2.9950\n" +
                    "cellsize 0.01\n" +
                    "nodata_value -9999.00\n");
            writer.flush();
            double[][] maskArray = new double[height][width];
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    if (i > 316 && i < 520 && j > 508 && j < 654) {
                        maskArray[i][j] = 1;
                        writer.write(maskArray[i][j] + "  ");
                        writer.flush();
                    } else {
                        maskArray[i][j] = -9999.00;
                        writer.write(maskArray[i][j] + "  ");
                        writer.flush();
                    }
                }
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            printError(e);
            System.exit(1);
        }
    }

    private static double[][] readAscii(String inputPath) {
        try {
            InputStream inputStream = new FileInputStream(new File(inputPath));
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            //初始化数组
            double[][] array = new double[height][width];

            String str ;
            int row = 0;
            //读取每一个文件的每一行
            while ((str = bufferedReader.readLine()) != null) {
                //从数据行开始
                if (row == 6) {
                    String[] currentData = str.split("\\s+");
                    // 将一维数组转换为为二维数组
                    String[][] currentDataArray = new String[height][width];
                    for (int x = 0; x < height; x++)
                        System.arraycopy(currentData, x * width, currentDataArray[x], 0, width);

                    // 对读取的每个String数据进行类型的转换
                    for (int i = 0; i < height; i++) {
                        for (int j = 0; j < width; j++) {
                            array[i][j] = Double.parseDouble(currentDataArray[i][j]);
                        }
                    }
                }
                row++;
            }
            bufferedReader.close();
            inputStreamReader.close();
            return array;
        } catch (IOException e) {
            printError(e);
            System.exit(1);
        }
        return null;
    }

    private static void setDirectory() {
        System.out.println("正在读取配置文件...");
        try {
            InputStream inputStream = new FileInputStream(configPath);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String str = bufferedReader.readLine();

            if (str == null) { //判断配置文件是否为空
                throw new Exception("未在DownloadConfiguration.txt中设置目录！");
            }

            String[] strArray = str.split(",");

            asciiDirectory = strArray[0];
            pngDirectory = strArray[1];
            // 验证目录的合法性
            if (asciiDirectory == null) { //判断配置文件是否为空
                throw new Exception("未在DownloadConfiguration.txt中设置ASCII数据文件的目录！");
            } else if (!new File(asciiDirectory).isDirectory()) { //判断目录是否存在且是否合法
                throw new Exception("ASCII数据文件目录不合法或不存在！");
            } else if (pngDirectory == null) {
                throw new Exception("未在DownloadConfiguration.txt中设置PNG图片的目录！");
            } else if (!new File(pngDirectory).isDirectory()) {
                throw new Exception("PNG图片目录不合法或不存在！");
            }
            System.out.println("ASCII数据文件的目录为：" + asciiDirectory);
            System.out.println("PNG图片的目录为：" + pngDirectory);

        } catch (Exception e) {
            e.printStackTrace();
            printError(e);
            System.exit(1);
        }
    }

    private static Color colorRule(double value) {
        if (value > 80.00) {
            return new Color(221, 0, 0);//[80,+∞)
        } else if (value > 64.00 || value == 64.00) {
            return new Color(254, 69, 162);//[64,80)
        } else if (value > 56.00 || value == 56.00) {
            return new Color(255, 134, 255);//[56,64)
        } else if (value > 48.00 || value == 48.00) {
            return new Color(255, 128, 0);//[48,56)
        } else if (value > 40.00 || value == 40.00) {
            return new Color(255, 255, 0);//[40,48)
        } else if (value > 32.00 || value == 32.00) {
            return new Color(124, 206, 2);//[32,40)
        } else if (value > 24.00 || value == 24.00) {
            return new Color(70, 255, 9);//[24,32)
        } else if (value > 16.00 || value == 16.00) {
            return new Color(0, 225, 12);//[16,24)
        } else if (value > 12.00 || value == 12.00) {
            return new Color(0, 179, 71);//[12,16)
        } else if (value > 8.00 || value == 8.00) {
            return new Color(0, 147, 117);//[8,12)
        } else if (value > 4.00 || value == 4.00) {
            return new Color(0, 6, 240);//[4,8)
        } else if (value > 2.00 || value == 2.00) {
            return new Color(0, 60, 108);//[2,4)
        } else if (value > 1.00 || value == 1.00) {
            return new Color(0, 119, 198);//[1,2)
        } else if (value > 0.10 || value == 0.10) {
            return new Color(0, 255, 255);//[0.1,1)
        } else if (value > 0.00 || value == 0.00) {
            return new Color(255, 255, 255);//[0,0.1)
        } else {
            return new Color(255, 255, 255, 0);//(-∞,0)
        }
    }

    private static int[][] getRainFallRgb(double[][] asciiArray) {
        // 根据设置的颜色对应规则，将Ascii文件的雨量数据转换成rgb值
        int[][] rgb = new int[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                rgb[i][j] = colorRule(asciiArray[i][j]).getRGB();
            }
        }
        return rgb;
    }

    private static void convert(String imageFile, int[][] rgbArray, int[][] mapRGB) {
        try {
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    //根据掩膜文件划定的范围、地图和降雨量的rgb值，生成结果图的rgb值
                    if (mapRGB[i][j] == new Color(255, 255, 255).getRGB() && rgbArray[i][j] != new Color(255, 255, 255).getRGB() && maskArray[i][j] != -9999.00) {
                        mapRGB[i][j] = rgbArray[i][j];
                    } else {
                        int red = new Color(mapRGB[i][j]).getRed();
                        int green = new Color(mapRGB[i][j]).getGreen();
                        int blue = new Color(mapRGB[i][j]).getBlue();
                        //如果red、green、blue三个值相等，则表示这个颜色是黑色，在地图上表示为分界线
                        //根据掩膜文件，在指定区域以外的降雨量数值为无效值，在地图上设置为白色
                        if (red == green && green == blue && maskArray[i][j] == -9999.00) {
                            mapRGB[i][j] = new Color(255, 255, 255).getRGB();
                        }
                    }
                }
            }
            //将二维数组转换为一维数组，作为结果图的rgb值
            int[] data = new int[height * width];
            for (int i = 0; i < height; i++)
                System.arraycopy(mapRGB[i], 0, data, i * width, width);
            BufferedImage mapBufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);
            mapBufferedImage.setRGB(0, 0, width, height, data, 0, width);
            File file = new File(imageFile);
            ImageIO.write(mapBufferedImage, "png", file);
        } catch (IOException e) {
            e.printStackTrace();
            printError(e);
            System.exit(1);
        }
    }

    private static void printError(Exception e) { //todo 增加控制台输出
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat();// 格式化时间
            simpleDateFormat.applyPattern("yyyyMMddHHmmss");
            Date date = new Date();// 获取当前时间

            String outputPath;
            if(pngDirectory == null || !new File(pngDirectory).isDirectory()){
                outputPath = "./" + simpleDateFormat.format(date) + "错误提示.txt";
            }else{
                outputPath = pngDirectory + "\\" + simpleDateFormat.format(date) + "错误提示.txt";
            }
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath, true), "GB2312"));
            writer.write(e.getMessage());
            writer.flush();
            writer.close();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    public static int[][] readMapRgb(String imageFile) {
        try {
            File file = new File(imageFile);
            if(!file.exists())throw new Exception("当前目录下找不到地图！");
            BufferedImage bufferedImage;
            bufferedImage = ImageIO.read(file);
            // 获取图片宽度和高度
            // 将图片sRGB数据写入一维数组
            int[] data = new int[width * height];
            bufferedImage.getRGB(0, 0, width, height, data, 0, width);

            // 将一维数组转换为为二维数组
            int[][] rgbArray = new int[height][width];
            for (int i = 0; i < height; i++)
                System.arraycopy(data, i * width, rgbArray[i], 0, width);
            return rgbArray;
        } catch (Exception e) {
            e.printStackTrace();
            printError(e);
            System.exit(1);
        }
        return null;
    }

}
