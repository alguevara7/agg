CREATE TABLE ad_counter (
  ad_id INT NOT NULL,
  type VARCHAR(5) NOT NULL,
  value INT NOT NULL,
  modification_date DATETIME NOT NULL,
  creation_date DATETIME NOT NULL,
  PRIMARY KEY (ad_id, type)) ENGINE=InnoDB;

CREATE TABLE ad_counter_offset (
  partition_id BIGINT NOT NULL,
  offset BIGINT NOT NULL,
  PRIMARY KEY (partition_id)) ENGINE=InnoDB;
