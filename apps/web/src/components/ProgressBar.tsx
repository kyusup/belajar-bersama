type Props = {
  percent: number;
  completed: number;
  total: number;
  label: string;
};

export function ProgressBar({ percent, completed, total, label }: Props) {
  return (
    <div className="progress-block">
      <p className="progress-label">
        {label}: {completed} / {total} ({percent}%)
      </p>
      <div
        className="progress-track"
        role="progressbar"
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={percent}
        aria-label={label}
      >
        <div className="progress-fill" style={{ width: `${percent}%` }} />
      </div>
    </div>
  );
}
