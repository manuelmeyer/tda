

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
				//println "adding ref $cellId=$line"
				def lines = refCells[cellId];
				if(lines == null) {
					def newLines = [];
					refCells[cellId] = newLines;
					lines = newLines;
				}
				refCells[cellId] << line;
			}
		}
		
		def clCells = [];
		
		new File(args[1]).eachLine { line ->
			clCells << line;
		}
		
		clCells.each {
			String[] cellLac = it.split(",")
			if(cellLac.length == 2) {
				def lac = "," + cellLac[0] + ",";
				def cellId = cellLac[1];
				def lines = refCells[cellId];
				if(lines != null) {
					lines.any { line -> 
						if(line.contains(lac)) {
							println "${line}";	
							return true;
						}
					}			
				} else {
					println " -- notfound cellId '${cellId}' lac '${lac}'"
				}
			} else {
				println " -- error on line ${it}"
			}
		}
	}
}
