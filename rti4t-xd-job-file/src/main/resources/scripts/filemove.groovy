import org.slf4j.LoggerFactory

def LOG = LoggerFactory.getLogger("MoveScript")

String fileIn = processedFile

LOG.info fileIn
def fileOut = fileIn.substring(0, fileIn.lastIndexOf("."))
LOG.info "Moving $fileIn to $fileOut"
new File(fileIn).renameTo(fileOut)


