package chat.model;

public class FileData {
    private String  fileName;
    private long    fileSize;
    private byte[]  data;
    private String  sender;
    private String  receiver;
    private boolean cloud;  // lưu cloud hay không

    public FileData() {}
    public FileData(String fileName, byte[] data, String sender, String receiver) {
        this.fileName = fileName; this.data = data;
        this.sender = sender;     this.receiver = receiver;
        this.fileSize = data.length;
    }

    public String  getFileName()           { return fileName; }
    public void    setFileName(String v)   { fileName = v; }
    public long    getFileSize()           { return fileSize; }
    public byte[]  getData()               { return data; }
    public void    setData(byte[] v)       { data = v; fileSize = v.length; }
    public String  getSender()             { return sender; }
    public String  getReceiver()           { return receiver; }
    public boolean isCloud()               { return cloud; }
    public void    setCloud(boolean v)     { cloud = v; }
}
