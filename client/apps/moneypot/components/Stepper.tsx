import { Icon } from './icons';

type State = 'done' | 'current' | 'todo';

/** A row of steps with the current one marked, for the stages of a round. */
export function Stepper({ steps, current }: { steps: { key: string; label: string }[]; current: string }) {
  const index = steps.findIndex((step) => step.key === current);
  return (
    <div className="stepper" role="list">
      {steps.map((step, i) => {
        const state: State = i < index ? 'done' : i === index ? 'current' : 'todo';
        return (
          <div className="step" data-state={state} key={step.key} role="listitem">
            <div className="step-dot">{state === 'done' ? <Icon name="check" size={16} /> : i + 1}</div>
            {step.label}
          </div>
        );
      })}
    </div>
  );
}
