import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Converts the existing PNG to native icons, without third-party build tools. */
class PackagingIcons {
    static byte[] png(BufferedImage source, int size) throws Exception {
        var image = new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB); var graphics=image.createGraphics();
        try { graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.drawImage(source,0,0,size,size,null); } finally { graphics.dispose(); }
        var bytes=new ByteArrayOutputStream();ImageIO.write(image,"png",bytes);image.flush();return bytes.toByteArray();
    }
    public static void main(String[] args) throws Exception {
        var source=ImageIO.read(Path.of(args[0]).toFile());Path out=Path.of(args[1]);Files.createDirectories(out);
        int[] sizes={16,24,32,48,64,128,256};var images=new ArrayList<byte[]>();for(int size:sizes) images.add(png(source,size));
        int length=6+16*sizes.length+images.stream().mapToInt(a->a.length).sum();var ico=ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);
        ico.putShort((short)0).putShort((short)1).putShort((short)sizes.length);int offset=6+16*sizes.length;
        for(int i=0;i<sizes.length;i++) {ico.put((byte)sizes[i]).put((byte)sizes[i]).put((byte)0).put((byte)0).putShort((short)1).putShort((short)32).putInt(images.get(i).length).putInt(offset);offset+=images.get(i).length;}
        for(byte[] image:images)ico.put(image);Files.write(out.resolve("fileflow.ico"),ico.array());
        var chunks=new ByteArrayOutputStream();var data=new DataOutputStream(chunks);
        String[] types={"ic07","ic08","ic09"};int[] macSizes={128,256,512};
        for(int i=0;i<types.length;i++){byte[] image=png(source,macSizes[i]);data.writeBytes(types[i]);data.writeInt(image.length+8);data.write(image);}
        try(var output=new DataOutputStream(Files.newOutputStream(out.resolve("fileflow.icns")))){output.writeBytes("icns");output.writeInt(chunks.size()+8);chunks.writeTo(output);}
        source.flush();
    }
}
