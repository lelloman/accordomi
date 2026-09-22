"""End-to-end WAV parsing and CLI export tests; Python standard library only."""
import json
import math
from pathlib import Path
import struct
import subprocess
import sys
import tempfile
import unittest

CLI = str(Path(sys.argv.pop(1)).resolve())


def wav(path, samples, bits=16, channels=1, floating=False, extra=False):
    data = bytearray()
    for sample in samples:
        for channel in range(channels):
            value = sample if channel == 0 else 0
            if floating:
                data.extend(struct.pack('<f', value))
            else:
                integer = int(value * ((1 << (bits - 1)) - 1))
                data.extend(integer.to_bytes(bits // 8, 'little', signed=True))
    fmt = struct.pack('<HHIIHH', 3 if floating else 1, channels, 44100,
                      44100 * channels * bits // 8, channels * bits // 8, bits)
    chunks = b'fmt ' + struct.pack('<I', len(fmt)) + fmt
    if extra:
        chunks += b'JUNK' + struct.pack('<I', 3) + b'abc\0'
    chunks += b'data' + struct.pack('<I', len(data)) + data
    if len(data) % 2:
        chunks += b'\0'
    path.write_bytes(b'RIFF' + struct.pack('<I', len(chunks) + 4) + b'WAVE' + chunks)


class CliTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.path = Path(self.directory.name) / 'note "test".wav'

    def run_cli(self, *args):
        return subprocess.run([CLI, str(self.path), '--midi', '57', *args],
                              text=True, capture_output=True)

    def test_encodings_channels_and_unknown_chunks(self):
        tone = [0.5 * math.sin(2 * math.pi * 220 * i / 44100) for i in range(8192)]
        for bits, floating in [(16, False), (24, False), (32, False), (32, True)]:
            wav(self.path, tone, bits, 2, floating, extra=True)
            for method in ['yin', 'autocorrelation', 'mcleod']:
                result = self.run_cli('--method', method)
                self.assertEqual(result.returncode, 0, result.stderr)
                rows = [json.loads(line) for line in result.stdout.splitlines()]
                self.assertEqual(len(rows), 5)
                self.assertAlmostEqual(rows[0]['frequency_hz'], 220, delta=0.1)
                self.assertEqual(rows[0]['source'], str(self.path))
                self.assertEqual(rows[-1]['start_sample'], 4096)
            self.assertEqual(self.run_cli('--method', 'yin', '--channel', '2').returncode, 3)

    def test_piano_export_and_csv(self):
        samples = []
        for i in range(65536):
            samples.append(sum(0.15 / n * math.sin(2 * math.pi * n * 220 *
                math.sqrt((1 + 0.0004 * n*n) / 1.0004) * i / 44100 + n * 0.7)
                for n in range(1, 11)))
        wav(self.path, samples)
        result = self.run_cli()
        self.assertEqual(result.returncode, 0, result.stderr)
        row = json.loads(result.stdout)
        self.assertEqual(row['status'], 'usable')
        self.assertAlmostEqual(row['B'], 0.0004, delta=0.00001)
        self.assertGreaterEqual(row['used_count'], 8)
        self.assertEqual(row['window_samples'], 65536)
        self.assertIn('residual_cents', row['partials'][0])
        csv = self.run_cli('--format', 'csv')
        self.assertEqual(csv.returncode, 0)
        self.assertIn('predicted_hz,residual_cents,used', csv.stdout)

    def test_target_cli_preserves_reference_and_rejects_duplicate_samples(self):
        samples = Path(self.directory.name) / 'samples.csv'
        samples.write_text('midi,B\n' + ''.join(f'{midi},0.0003\n' for midi in [93, 21, 45, 33, 69, 81, 88]))
        command = [str(Path(CLI).with_name('accordomi-targets')), str(samples), '--reference', '442']
        result = subprocess.run(command, text=True, capture_output=True)
        self.assertEqual(result.returncode, 0, result.stderr)
        rows = [json.loads(line) for line in result.stdout.splitlines()]
        self.assertEqual(len(rows), 88)
        self.assertEqual(rows[48]['target_hz'], 442)
        self.assertLess(rows[0]['stretch_cents'], 0)
        self.assertGreater(rows[-1]['stretch_cents'], 0)
        samples.write_text(samples.read_text() + '45,0.0003\n')
        self.assertEqual(subprocess.run(command, capture_output=True).returncode, 2)

    def test_bad_input_and_short_recordings(self):
        wav(self.path, [0.0] * 100)
        self.assertEqual(self.run_cli().returncode, 3)
        for args in [('--midi', 'NaN'), ('--hop', '999999'), ('--window', '1'),
                     ('--reference', 'inf'), ('--channel', '2'), ('--unknown', '1')]:
            self.assertEqual(self.run_cli(*args).returncode, 2)
        self.path.write_bytes(self.path.read_bytes()[:-1])
        self.assertEqual(self.run_cli().returncode, 2)
        self.path.write_bytes(b'not a WAV')
        self.assertEqual(self.run_cli().returncode, 2)
        wav(self.path, [float('nan')] * 4096, bits=32, floating=True)
        self.assertEqual(self.run_cli('--method', 'yin').returncode, 2)


if __name__ == '__main__':
    unittest.main()
