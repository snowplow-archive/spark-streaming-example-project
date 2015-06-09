Vagrant.configure("2") do |config|

  config.vm.box = "ubuntu/trusty64"
  config.vm.hostname = "spark-streaming-example-project"
  config.ssh.forward_agent = true

  # Forward guest port 4040 to host port 4040 (for Spark web UI)
  config.vm.network "forwarded_port", guest: 4040, host: 4040

  config.vm.provider :virtualbox do |vb|
    vb.name = Dir.pwd().split("/")[-1] + "-" + Time.now.to_f.to_i.to_s
    vb.customize ["modifyvm", :id, "--natdnshostresolver1", "on"]
    vb.customize [ "guestproperty", "set", :id, "--timesync-threshold", 10000 ]
    # Scala is memory-hungry
    vb.memory = 8000
  end

  config.vm.provision :shell do |sh|
    sh.path = "vagrant/up.bash"
  end

end
