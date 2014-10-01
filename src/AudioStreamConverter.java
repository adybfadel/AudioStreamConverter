import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AudioStreamConverter extends Thread {

	private String audioStream;
	private static final String URL_AUDIO_STREAM = "/usr/local/WowzaStreamingEngine/content/sonyGuruAudio";
	private static List<String> listStreams = new ArrayList<String>();
	private static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:MM:ss");

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
				for (File file : dir.listFiles()) {
					String fileName = file.getName();
					if (!fileName.endsWith(".mp4") || fileName.indexOf("_") > -1)
						continue;
					fileName = fileName.replace(".mp4", "");
					if (!listStreams.contains(fileName)) {
						listStreams.add(fileName);
						new AudioStreamConverter(fileName).start();
					}
				}
				Thread.sleep(500);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}

	@Override
	public void run() {

		String urlAudio = "rtmp://localhost/sonyGuruAudio";
		String urlFile = URL_AUDIO_STREAM + "/" + audioStream + ".mp4";
		String cmd = String.format("sudo /opt/ffmpeg/ffmpeg -i %s/%s -acodec libfaac -f flv %s/%s_android", urlAudio, audioStream, urlAudio, audioStream);

		try {
			System.out.println(String.format("[%s] Converting: %s", sdf.format(new Date()), audioStream));
			Process process = Runtime.getRuntime().exec(cmd);
			process.waitFor();
			Thread.sleep(500);
			process = Runtime.getRuntime().exec("rm -rf " + urlFile.replace(".mp4", "_android*.mp4"));
			process.waitFor();
			process = Runtime.getRuntime().exec("mv -f " + urlFile.replace(".mp4", "*.mp4") + " " + URL_AUDIO_STREAM + "/bkp");
			process.waitFor();
			System.out.println(String.format("[%s] Finished: %s", sdf.format(new Date()), audioStream));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		File file = new File(urlFile);
		if (file.isFile())
			listStreams.remove(audioStream);
	}

}
