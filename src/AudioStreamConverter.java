import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AudioStreamConverter extends Thread {

	private static final String URL_AUDIO_STREAM = "/usr/local/WowzaStreamingEngine/content/sonyGuruAudio";
	private static final String LOG_FILE_NAME = "/opt/sonyguru/AudioStreamConverter.log";
	private static final long LOG_FILE_SIZE = 5000000;

	private String audioStream;
	private static List<String> listStreams = new ArrayList<String>();
	private static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

	private static File logFile = null;
	
	public AudioStreamConverter(String audioSream) {
		this.audioStream = audioSream;
	}

	public static void main(String[] args) {
		
		if (args.length == 0 || !"-v".equals(args[0])) {
			try {
				logFile = new File(LOG_FILE_NAME);
				if (logFile.exists()) {
					copyFile(logFile, new File(logFile.getAbsolutePath().replace(".log", "_" + logFile.lastModified() + ".log")));
					logFile.delete();
					logFile.createNewFile();
				}
				System.setOut(new PrintStream(logFile));
			} catch (Exception e) {
				System.out.println("Error opening log file: " + logFile.getAbsolutePath());
				e.printStackTrace();
			}
		}

		try {
			log("Audio converter started...");
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
	
	private static void log(String log) {
		System.out.println(String.format("[%s] %s", sdf.format(new Date()), log));
		if (logFile != null && logFile.length() >= LOG_FILE_SIZE) {
			try {
				copyFile(logFile, new File(logFile.getAbsolutePath().replace(".log", "_" + System.currentTimeMillis() + ".log")));
				logFile.delete();
				logFile.createNewFile();
			} catch (IOException e) {
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
//			log(String.format("Converting: %s - %s", audioStream, command));
			Process process = Runtime.getRuntime().exec(command);
			process.waitFor();
//			log(String.format("Finished: %s", audioStream));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		File file = new File(urlFile);
		if (file.isFile())
			listStreams.remove(audioStream);
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
    
}
