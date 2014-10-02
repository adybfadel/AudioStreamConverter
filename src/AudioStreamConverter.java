import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AudioStreamConverter extends Thread {

	private String audioStream;
	private static final String URL_AUDIO_STREAM = "/usr/local/WowzaStreamingEngine/content/sonyGuruAudio";
	private static List<String> listStreams = new ArrayList<String>();
	private static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

	public AudioStreamConverter(String audioSream) {
		this.audioStream = audioSream;
	}

	public static void main(String[] arqs) {

		try {
			System.out.println(String.format("[%s] Audio converter started...", sdf.format(new Date())));
			Runtime.getRuntime().exec("sudo su");
		} catch (Exception e1) {
			e1.printStackTrace();
			System.exit(1);
		}
		
		while (true) {

			try {
				File dir = new File(URL_AUDIO_STREAM);
				if (dir.listFiles() != null) {
					for (File file: dir.listFiles()) {
						String fileName = file.getName();
						// Trata somente os streamings correntes e nao os de backup ou convertidos
						if (!fileName.endsWith(".mp4") || fileName.indexOf("_") > -1)
							continue;
						fileName = fileName.replace(".mp4", "");
						if (!listStreams.contains(fileName)) {
							listStreams.add(fileName);
							new AudioStreamConverter(fileName).start();
						}
					}
				}
				Thread.sleep(1000);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}

	@Override
	public void run() {

		String urlAudio = "rtmp://localhost/sonyGuruAudio";
		String urlFile = URL_AUDIO_STREAM + "/" + audioStream + ".mp4";
		String command = String.format("/opt/ffmpeg/ffmpeg -i %s/%s -acodec libfaac -f flv %s/%s_android", urlAudio, audioStream, urlAudio, audioStream);

		try {
			System.out.println(String.format("[%s] Converting: %s - %s", sdf.format(new Date()), audioStream, command));
			Process process = Runtime.getRuntime().exec(command);
			process.waitFor();
			//pigeonhole(audioStream);
			System.out.println(String.format("[%s] Finished: %s", sdf.format(new Date()), audioStream));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		File file = new File(urlFile);
		if (file.isFile())
			listStreams.remove(audioStream);
	}
	
	private void pigeonhole(final String filter) {
		
		File dir = new File(URL_AUDIO_STREAM);
		File[] files = dir.listFiles(new FileFilter(filter));
		for (File file: files) {
			if (!file.isDirectory()) {
				try {
					// So copia o arquivo principal
					if (file.getName().equals(filter + ".mp4")) {
						String dest = URL_AUDIO_STREAM + "/bkp/" + file.getName();
						System.out.println("Copying " + file.getName() + " to " + dest);
						copyFile(file, new File(dest));
					}
					boolean del = file.delete();
					System.out.println("Removing " + file.getName() + " - " + del);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}

    public static void copyFile(File source, File destination) throws IOException {
        if (destination.exists())
            destination.delete();
        destination.createNewFile();

        FileChannel sourceChannel = null;
        FileChannel destinationChannel = null;

        try {
            sourceChannel = new FileInputStream(source).getChannel();
            destinationChannel = new FileOutputStream(destination).getChannel();
            sourceChannel.transferTo(0, sourceChannel.size(),
                    destinationChannel);
        } finally {
            if (sourceChannel != null && sourceChannel.isOpen())
                sourceChannel.close();
            if (destinationChannel != null && destinationChannel.isOpen())
                destinationChannel.close();
       }
    }
    
    private class FileFilter implements FilenameFilter {
    	
    	private String prefix;
    	
    	public FileFilter(String prefix) {
    		this.prefix = prefix;
    	}
    	
    	public boolean accept(File dir, String name) {
			String lowercaseName = name.toLowerCase();
			if (lowercaseName.startsWith(prefix.toLowerCase()) && lowercaseName.endsWith(".mp4")) {
				return true;
			} else {
				return false;
			}
		}
    	
    }
	
}
