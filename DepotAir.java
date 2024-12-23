import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

// interface
interface LayananDepot {
    void tampilkanInfo() throws SQLException;
}


// Kelas induk
class Stok {
    protected Connection connection;
    private static final int KAPASITAS_MAX = 1000;

    public Stok(Connection connection) {
        this.connection = connection;
    }

    public void tambahToren(String toren, int jumlah) throws SQLException {
        String query = "SELECT stok FROM toren WHERE nama_toren = ?";
        PreparedStatement stmt = connection.prepareStatement(query);
        stmt.setString(1, toren);
        ResultSet rs = stmt.executeQuery();
        
        if (rs.next()) {
            int stokSekarang = rs.getInt("stok");
            if (stokSekarang + jumlah > KAPASITAS_MAX) {
                System.out.println("Toren " + toren + " sudah penuh. Tidak bisa menambah air lebih dari kapasitas.");
            } else {
                String updateQuery = "UPDATE toren SET stok = stok + ? WHERE nama_toren = ?";
                PreparedStatement updateStmt = connection.prepareStatement(updateQuery);
                updateStmt.setInt(1, jumlah);
                updateStmt.setString(2, toren);
                updateStmt.executeUpdate();

                // Simpan riwayat penambahan air
                String logQuery = "INSERT INTO riwayat_tambah_air (nama_toren, jumlah, waktu) VALUES (?, ?, NOW())";
                PreparedStatement logStmt = connection.prepareStatement(logQuery);
                logStmt.setString(1, toren);
                logStmt.setInt(2, jumlah);
                logStmt.executeUpdate();

                System.out.println("Stok Toren " + toren + " berhasil ditambahkan. Jumlah stok saat ini: " + (stokSekarang + jumlah) + " liter.");
            }
        }
    }

    public int getTotalAir() throws SQLException {
        String query = "SELECT SUM(stok) AS total_air FROM toren";
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(query);
        if (rs.next()) {
            return rs.getInt("total_air");
        }
        return 0;
    }

    public void tampilkanRiwayatTambahAir() throws SQLException {
        String query = "SELECT * FROM riwayat_tambah_air ORDER BY waktu DESC";
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(query);
        System.out.println("Riwayat Penambahan Air:");
        while (rs.next()) {
            System.out.println("Toren: " + rs.getString("nama_toren") + ", Jumlah: " + rs.getInt("jumlah") + " liter, Waktu: " + rs.getTimestamp("waktu"));
        }
    }
}

// Kelas anak
class Depot extends Stok implements LayananDepot {
    private String namaDepot;

    public Depot(Connection connection, String namaDepot) {
        super(connection);
        this.namaDepot = namaDepot;
    }

    @Override
    public void tampilkanInfo() throws SQLException {
        System.out.println("Nama Depot: " + namaDepot);
        String query = "SELECT * FROM toren";
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(query);
        while (rs.next()) {
            System.out.println("Stok Toren " + rs.getString("nama_toren") + " (Liter): " + rs.getInt("stok"));
        }
        System.out.println("Total Stok Air (Liter): " + getTotalAir());
    }
}

// Kelas Pelanggan
class Pelanggan {
    private Connection connection;

    public Pelanggan(Connection connection) {
        this.connection = connection;
    }

    public void tambahPesanan(String nama, String alamat, int jumlahSmall, int jumlahLarge, String tanggalAntar) throws SQLException {
        int totalAir = (jumlahSmall * 10) + (jumlahLarge * 20);

        String query = "INSERT INTO pesanan (nama, alamat, jumlah_small, jumlah_large, tanggal_antar) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement stmt = connection.prepareStatement(query);
        stmt.setString(1, nama);
        stmt.setString(2, alamat);
        stmt.setInt(3, jumlahSmall);
        stmt.setInt(4, jumlahLarge);
        stmt.setString(5, tanggalAntar);
        stmt.executeUpdate();

        System.out.println("Pesanan berhasil ditambahkan.");
    }

    public void tampilkanPesanan() throws SQLException {
        String query = "SELECT * FROM pesanan";
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(query);
        while (rs.next()) {
            System.out.println("Nama: " + rs.getString("nama") + ", Alamat: " + rs.getString("alamat") + ", Small: " + rs.getInt("jumlah_small") + ", Large: " + rs.getInt("jumlah_large") + ", Tanggal Antar: " + rs.getString("tanggal_antar"));
            System.out.println("-------------------------");
        }
    }

    public void kurangiStok(String jenisGalon, int jumlah) throws SQLException {
        int pengurangan = 0;
    
        // Tentukan pengurangan berdasarkan jenis galon
        if (jenisGalon.equalsIgnoreCase("small")) {
            pengurangan = 10 * jumlah; // 10 liters per small gallon
        } else if (jenisGalon.equalsIgnoreCase("large")) {
            pengurangan = 20 * jumlah; // 20 liters per large gallon
        } else {
            System.out.println("Jenis galon tidak valid.");
            return;
        }
    
        // Ambil stok saat ini
        String query = "SELECT stok FROM toren WHERE nama_toren = ?";
        PreparedStatement stmt = connection.prepareStatement(query);
        stmt.setString(1, "nama_toren"); // Ganti dengan nama toren yang sesuai
        ResultSet rs = stmt.executeQuery();
    
        if (rs.next()) {
            int stokSekarang = rs.getInt("stok");
            if (stokSekarang < pengurangan) {
                System.out.println("Stok tidak cukup untuk memenuhi permintaan.");
            } else {
                // Kurangi stok
                String updateQuery = "UPDATE toren SET stok = stok - ? WHERE nama_toren = ?";
                PreparedStatement updateStmt = connection.prepareStatement(updateQuery);
                updateStmt.setInt(1, pengurangan);
                updateStmt.setString(2, "nama_toren"); // Ganti dengan nama toren yang sesuai
                updateStmt.executeUpdate();
    
                // Simpan riwayat penggunaan air
                String logQuery = "INSERT INTO riwayat_penggunaan_air (nama_toren, jumlah, waktu) VALUES (?, ?, NOW())";
                PreparedStatement logStmt = connection.prepareStatement(logQuery);
                logStmt.setString(1, "nama_toren"); // Ganti dengan nama toren yang sesuai
                logStmt.setInt(2, pengurangan);
                logStmt.executeUpdate();
    
                System.out.println("Stok berhasil dikurangi. Jumlah stok saat ini: " + (stokSekarang - pengurangan) + " liter.");
            }
        }
    }
}

public class DepotAir {
    public static void main(String[] args) {
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/depot_air", "root", "")) {
            Scanner scanner = new Scanner(System.in);
            Depot depot = new Depot(connection, "Depot Air Segar");
            Pelanggan pelanggan = new Pelanggan(connection);
            

            while (true) {
                System.out.println("-------------------------------------");
                System.out.println("            JON WATER                       ");
                System.out.println("-------------------------------------");

                System.out.println("\nMenu:");
                System.out.println("1. Tambah Air ke Toren 1");
                System.out.println("2. Tambah Air ke Toren 2");
                System.out.println("3. Tambah Pesanan Pelanggan");
                System.out.println("4. Tampilkan Pesanan Pelanggan");
                System.out.println("5. Tampilkan Stok Air");
                System.out.println("6. Tampilkan Riwayat Penambahan Air");
                System.out.println("7. Keluar");
                System.out.print("Pilih opsi: ");

                int pilihan = scanner.nextInt();
                scanner.nextLine();

                switch (pilihan) {
                    case 1:
                        System.out.print("Masukkan jumlah air untuk Toren 1 (Liter): ");
                        int jumlahToren1 = scanner.nextInt();
                        depot.tambahToren("Toren1", jumlahToren1);
                        break;
                    case 2:
                        System.out.print("Masukkan jumlah air untuk Toren 2 (Liter): ");
                        int jumlahToren2 = scanner.nextInt();
                        depot.tambahToren("Toren2", jumlahToren2);
                        break;
                    case 3:
                        System.out.print("Masukkan nama pelanggan: ");
                        String nama = scanner.nextLine();
                        System.out.print("Masukkan alamat pengantaran: ");
                        String alamat = scanner.nextLine();
                        System.out.print("Masukkan jumlah galon kecil (Small): ");
                        int jumlahSmall = scanner.nextInt();
                        System.out.print("Masukkan jumlah galon besar (Large): ");
                        int jumlahLarge = scanner.nextInt();
                        scanner.nextLine(); // Konsumsi newline
                        System.out.print("Masukkan tanggal pengantaran (yyyy-MM-dd): ");
                        String tanggalAntar = scanner.nextLine();
                        pelanggan.tambahPesanan(nama, alamat, jumlahSmall, jumlahLarge, tanggalAntar);
                        break;
                    case 4:
                        pelanggan.tampilkanPesanan();
                        break;
                    case 5:
                        depot.tampilkanInfo();
                        break;
                    case 6:
                        depot.tampilkanRiwayatTambahAir();
                        break;
                    case 7:
                        System.out.println("Keluar dari program. Terima kasih!");
                        return;
                    default:
                        System.out.println("Pilihan tidak valid. Silakan coba lagi.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: " + e.getMessage());
        }
}
    }

