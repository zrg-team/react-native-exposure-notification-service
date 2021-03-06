package ie.gov.tracing.storage;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import net.sqlcipher.database.SupportFactory;

import java.util.Date;
import java.util.UUID;

import ie.gov.tracing.common.Events;

@Database(
    entities = {
      ExposureEntity.class,
      TokenEntity.class
    },
    version = 1,
    exportSchema = false)
@TypeConverters({ZonedDateTimeTypeConverter.class})
public abstract class ExposureNotificationDatabase extends RoomDatabase {
  private static final String DATABASE_NAME = "exposurenotifications_encrypted";

  @SuppressWarnings("ConstantField") // Singleton pattern.
  private static volatile ExposureNotificationDatabase INSTANCE;
  abstract ExposureDao exposureDao();
  abstract TokenDao tokenDao();

  static synchronized ExposureNotificationDatabase getInstance(Context context) {
    if (INSTANCE == null) {
      Events.raiseEvent(Events.INFO, "buildDatabase - start: " + new Date());
      INSTANCE = buildDatabase(context);
      Events.raiseEvent(Events.INFO, "buildDatabase - done: " + new Date());
    }
    return INSTANCE;
  }

  private static ExposureNotificationDatabase buildDatabase(Context context) {
    try {
      String password = SharedPrefs.getString("password", context);

      if (password.isEmpty()) {
        Events.raiseEvent(Events.INFO, "buildDatabase - no password, creating...");
        nukeDatabase(context); // just in case we had previous one, new password = new database
        password = UUID.randomUUID().toString();
        SharedPrefs.setString("password", password, context);
        Events.raiseEvent(Events.INFO, "buildDatabase - password set");
      }
      Events.raiseEvent(Events.INFO, "buildDatabase - building...");
      SupportFactory sqlcipherFactory = new SupportFactory(password.getBytes());
      return Room.databaseBuilder(
              context.getApplicationContext(), ExposureNotificationDatabase.class, DATABASE_NAME).openHelperFactory(sqlcipherFactory)
              .build();
    }
    catch (Exception ex) {
      Events.raiseError("buildDatabase", ex);
    }
    return null;
  }

  public static void nukeDatabase (Context context) {
    try {
      Events.raiseEvent(Events.INFO, "Nuking database");
      context.getApplicationContext().deleteDatabase(DATABASE_NAME);
      INSTANCE = null;
      Events.raiseEvent(Events.INFO, "Database nuked");
    } catch(Exception e) {
      Events.raiseError("Error nuking database", e);
    }
  }
}
