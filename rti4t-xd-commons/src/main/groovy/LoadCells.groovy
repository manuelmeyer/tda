

public class LoadCells {
	
	static public void main(String[] args) {
		new LoadCells().run(args);
	}

	private void run(String[] args) {
		def refCells = [:]
		new File(args[0]).eachLine { line ->
			def matcher = line =~ /^(\d+),/
			if(matcher.find()) {
				def cellId = matcher.group(1);
				refCells[cellId] = line
			}
		}
		
		def clCells = [];
		
		new File(args[1]).eachLine { line ->
			String[] lacCells = line.split(",");
			if(lacCells.length != 2) {
				println "Error on $line";
			}
			String[] cells = lacCells[1].split(";");
			String lac = lacCells[0];
			cells.each { 
				def cellLac = it.trim() + "," + lac.trim(); 
				//println "Adding ${cellLac}"
				clCells << cellLac; 
			}
		}
		
		clCells.each {
			String[] cellLac = it.split(",")
			if(cellLac.length == 2) {
				def lac = "," + cellLac[1] + ",";
				def line = refCells[cellLac[0]];
				if(line != null && line.contains(lac)) {
					println " -- found ${line}";					
				} else {
					println " -- notfound cellId '${cellLac[0]}' lac '${cellLac[1]}'"
				}
			}
		}
	}
}
