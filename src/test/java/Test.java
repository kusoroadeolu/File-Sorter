import java.io.IOException;
import java.nio.file.*;
import java.sql.SQLOutput;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;

public class Test {
  public static void main(String[] args) {
      try(WatchService watchService = FileSystems.getDefault().newWatchService()){
          Path path = Paths.get("");
          path.register(watchService, StandardWatchEventKinds.ENTRY_DELETE,
                  StandardWatchEventKinds.ENTRY_CREATE,
                  StandardWatchEventKinds.ENTRY_MODIFY);

          while(true){
            WatchKey key = watchService.take();
            for(WatchEvent<?> eventKind:  key.pollEvents()){
              WatchEvent.Kind<?> kind = eventKind.kind();
              System.out.println(eventKind.context() + ": " + kind);
            }
          }



      } catch (IOException e) {
          throw new RuntimeException(e);
      } catch (InterruptedException e) {
          throw new RuntimeException(e);
      }
  }
}





