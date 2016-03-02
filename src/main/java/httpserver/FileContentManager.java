package httpserver;

import java.io.*;
import java.net.URI;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;

import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FileContentManager {

    /**
     * The file cache which contains buffers of files which have been requested.
     */
    private Map<String, MappedByteBuffer> fileCache = new ConcurrentHashMap<>();
    private boolean useCache;
    private Path rootDir;


    public FileContentManager(Path _rootDir, boolean _useCache) {
        rootDir = _rootDir;
        useCache = _useCache;

        if (useCache) {
            try {
                reloadFileCache();
                fileCacheWatchService();
            } catch (IOException e) {
                System.err.println("Can't start caching!");
                System.exit(-1);
            }
        }
    }

    public MappedByteBuffer getFileContent(String path) throws IOException
    {
        // Caching
        if (useCache && fileCache.containsKey(path)) {
            return fileCache.get(path);
        }

        // No caching or cache does not contain resource for some reason
        File file = new File(rootDir.getFileName().toFile(), path);
        FileChannel fc = new RandomAccessFile(file, "r").getChannel();
        MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
        //System.out.println(file.getName() + " " + mbb.remaining() + " " + file.length());
        return mbb;
    }

    private void reloadFileCache() throws IOException {
        try {
            Files.walkFileTree(rootDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
                        throws IOException  {

                    // TODO: 01.03.2016  dirty hack with adding "/"
                    String relPath = ("/" + rootDir.relativize(path)).replace('/', File.separatorChar);

                    // Очистка, чтобы не забирать из того же кэша
                    if (fileCache.containsKey(relPath)) {
                        //fileCache.get(relPath).reset();
                        fileCache.remove(relPath);
                    }

                    fileCache.put(relPath, getFileContent(relPath));
                    return FileVisitResult.CONTINUE;
                }
            });
            System.out.println("Cache is up-to-date. " +
                    "Cached Files: " + fileCache.size());
        } catch (IOException e) {
            System.err.println("Cache updating error!");
        }
    }

    private void fileCacheWatchService() throws IOException {
        new Thread(() -> {
            WatchService watcher = null;
            try {
                watcher = rootDir.getFileSystem().newWatchService();
                rootDir.register(watcher,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (true) {
                WatchKey key = null;
                try {
                    key = watcher.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (!key.pollEvents().isEmpty()) {
                    try {
                        // TODO: 29.02.2016
                        reloadFileCache();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                key.reset();
            }
        }).start();
    }
}
