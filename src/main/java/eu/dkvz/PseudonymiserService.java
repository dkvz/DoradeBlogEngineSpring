package eu.dkvz;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.Arrays;
import java.util.stream.Stream;

import eu.dkvz.utils.*;

@Service
public class PseudonymiserService {

	@Value("${pseudos-path}")
	private String filename;
	private int count = 0;
    private double increment = 1.0f;
    
    @PostConstruct
	public void initialize() {
    	this.countLines();
    }
    
    private void countLines() {
        try
        (
           FileReader       input = new FileReader(this.filename);
           LineNumberReader count = new LineNumberReader(input);
        ) {
           while (count.skip(Long.MAX_VALUE) > 0)
           {
              // Loop just in case the file is > Long.MAX_VALUE or skip() 
              // decides to not read the entire file
           }
           this.count = count.getLineNumber();
           // Also set the "increment":
           this.increment = Long.MAX_VALUE / Math.ceil(this.count / 2.0d);
        } catch (IOException ex) {
            this.count = 0;
        }
    }
    
    public String hashAndFind(String val) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] res = md.digest(val.getBytes());
        // Take the 8 first bytes (64bit):
        long l = ByteUtils.bytesToLong(Arrays.copyOfRange(res, 0, 8));
        return this.findName(l);
    }
    
    private String readLine(long line) throws IOException {
        try (Stream<String> lines = Files.lines(Paths.get(this.filename))) {
            return lines.skip(line).findFirst().get();
        }
    }
    
    private String findName(long val) throws IOException {
        if (val < 0) {
            // We look in the first half of the whole list.
            return this.readLine(Math.abs(Math.round(val / this.increment)));
        } else {
            // We look in the second half of the whole list.
            long line = Math.round(val / this.increment) + (long)Math.ceil(this.count / 2.0d);
            if (line >= this.count) {
                // Set it to the last line if we go over it.
                line = this.count - 1;
            }
            return this.readLine(line);
        }
    }

	public String getFilename() {
		return filename;
	}

	public int getCount() {
		return count;
	}

	public double getIncrement() {
		return increment;
	}
	
}
