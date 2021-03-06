//package eu.stamp_project.reneri.observations;
//
//import java.io.*;
//import java.nio.file.Path;
//import java.util.*;
//import java.util.stream.Stream;
//
//public class ObservationCollection {
//
//    private final static String OBSERVATIONS_JSONL = "observations.jsonl";
//
//    private HashMap<String, PointObservationCollection> pointObservations = new HashMap<>();
//
//    private Path folderPath;
//
//    public ObservationCollection(Path folder) throws InvalidObservationFileException {
//        fromFolder(folder);
//        this.folderPath = folder;
//
//
//    }
//
//    private void fromFolder(Path folderPath) throws InvalidObservationFileException {
//        File folder = folderPath.toFile();
//
//        File[] directories = folder.listFiles((file) -> file.isDirectory() && file.canRead());
//
//        if(directories == null) {
//            // Empty folder
//            return;
//        }
//        Iterable<File> observationFiles = Arrays.stream(directories)
//                .map(dir -> dir.toPath().resolve(OBSERVATIONS_JSONL).toFile())
//                .filter(file -> file.exists() && file.canRead())
//                ::iterator;
//        for(File file : observationFiles) {
//            try {
//                addGroups(loadGroupedObservations(file));
//            }
//            catch(IOException exc) {
//                throw new InvalidObservationFileException("Error while reading the file", file.getAbsolutePath(), exc);
//            }
//            catch (InvalidObservationException exc) {
//                throw new InvalidObservationFileException("Invalid observation record", file.getAbsolutePath(), exc);
//            }
//        }
//    }
//
//    private HashMap<String, List<Observation>> loadGroupedObservations(File file) throws IOException, InvalidObservationException {
//
//        FileReader fileReader = new FileReader(file);
//        BufferedReader reader = new BufferedReader(fileReader);
//
//        HashMap<String, List<Observation>> groups = new HashMap<>();
//
//        String line;
//        while((line = reader.readLine()) != null) {
//            Observation observation = Observation.fromString(line);
//
//            List<Observation> observationList = groups.getOrDefault(observation.getPointcut(), null);
//            if(observationList != null) {
//                observationList.add(observation);
//            }
//            else {
//                observationList = new ArrayList<>();
//                observationList.add(observation);
//                groups.put(observation.getPointcut(), observationList);
//            }
//        }
//        reader.close();
//        fileReader.close(); //TODO: Needed?
//        return groups;
//    }
//
//    private void addGroups(HashMap<String, List<Observation>> groups) {
//        for(String pointcut : groups.keySet()) {
//            PointObservationCollection observationCollection = pointObservations.getOrDefault(pointcut, null);
//            if(observationCollection == null) {
//                pointObservations.put(pointcut, new PointObservationCollection(pointcut, groups.get(pointcut)));
//            }
//            else {
//                pointObservations.get(pointcut).addArchive(groups.get(pointcut));
//            }
//        }
//    }
//
//    public Set<String> getObservedPoints() {
//        return pointObservations.keySet();
//    }
//
//    public boolean has(String pointcut) {
//        return pointObservations.containsKey(pointcut);
//    }
//
//    public PointObservationCollection get(String pointcut) {
//        return pointObservations.get(pointcut);
//    }
//
//    public Path getFolderPath() {
//        return folderPath;
//    }
//}
