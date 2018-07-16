package eu.dkvz;

import java.io.*;
import eu.dkvz.BlogAuthoring.model.*;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;

@Service
public class GeoIPService {
	
	@Value("${geoip-path}")
    private String geoipFilename;
	
	private DatabaseReader geoReader = null;
	
	@PostConstruct
	public void initialize() throws IOException {
		File database = new File(this.geoipFilename);
		if (database.exists()) {
			this.geoReader = new DatabaseReader.Builder(database).build();
			System.out.println("GeoIP database initialized.");
		} else {
			System.out.println("GeoIP database not found.");
		}
	}

	public GeoInfo getLocalization(String ip) {
		GeoInfo res = new GeoInfo();
		if (this.geoReader != null) {
			try {
				InetAddress ipAddress = InetAddress.getByName(ip);
				CityResponse response = geoReader.city(ipAddress);
				if (response.getCountry() != null) {
					res.setCountry(response.getCountry().getName());
				}
				if (response.getCity() != null) {
					res.setCity(response.getCity().getName());
				}
				//res += response.getPostal();
				if (response.getMostSpecificSubdivision() != null) {
					res.setRegion(response.getMostSpecificSubdivision().getName());
				}
				return res;
			} catch (UnknownHostException ex) {
				return res;
			} catch (GeoIp2Exception ex) {
				return res;
			} catch (IOException ex) {
				return res;
			}
		} else {
			return res;
		}
	}
	
}
