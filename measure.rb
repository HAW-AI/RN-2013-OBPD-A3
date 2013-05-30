#!/usr/bin/env ruby

window_sizes = [1, 8, 128]
error_rates = [10, 100, 10000]

if ARGV.size < 3
  puts "the required parameters are:"
  puts "hostname of the server"
  puts "sourcepath including filename of the file you want to send"
  puts "targetpath including filename of the file you want to receive"
end

class Time
  def to_ms
    (self.to_f * 1000.0).to_i
  end
end

window_sizes_results = {}
window_sizes.each do |window|
  puts "Window size: #{window}"

  error_rates_results = {}
  error_rates.each do |error|
    puts "  Error rate: #{error}"

    attempt_results = []
    3.times do |attempt|
      puts "    Attempt: #{attempt+1}"

      start_time = Time.now

      output_file_name="#{ARGV[2]}.window_size_#{window}.error_rate_#{error}.attempt_#{attempt+1}"
      # cannot get rid of this annoying output even with > /dev/null
      system("java FileCopyClient #{ARGV[0]} #{ARGV[1]} #{output_file_name} #{window} #{error} > /dev/null")
      #sleep 1

      end_time = Time.now

      time_result_in_ms = end_time.to_ms - start_time.to_ms
      attempt_results << time_result_in_ms

      puts "    #{time_result_in_ms}ms"
      puts ""

      # wait for the server to be ready again
      sleep 5
    end

    average_time = attempt_results.empty? ? 0 : (attempt_results.reduce(&:+) / attempt_results.size)

    error_rates_results[error] = average_time
  end

  window_sizes_results[window] = error_rates_results
end

window_sizes_results.each do |wrk, wrv|
  puts "Window size: #{wrk}"
  wrv.each do |erk, erv|
    puts "  Error rate: #{erk}. Average time over 3 attempts: #{erv}"
  end
end
